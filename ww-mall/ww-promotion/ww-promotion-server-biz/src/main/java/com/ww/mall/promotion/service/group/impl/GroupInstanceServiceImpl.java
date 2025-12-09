package com.ww.mall.promotion.service.group.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.ThreadUtil;
import com.ww.app.rabbitmq.RabbitMqPublisher;
import com.ww.app.redis.component.lua.RedisScriptComponent;
import com.ww.mall.promotion.controller.app.group.req.CreateGroupRequest;
import com.ww.mall.promotion.controller.app.group.req.JoinGroupRequest;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.entity.group.GroupInstance;
import com.ww.mall.promotion.entity.group.GroupMember;
import com.ww.mall.promotion.enums.GroupMemberStatus;
import com.ww.mall.promotion.enums.GroupStatus;
import com.ww.mall.promotion.key.GroupRedisKeyBuilder;
import com.ww.mall.promotion.mq.GroupMqConstant;
import com.ww.mall.promotion.mq.GroupRefundMessage;
import com.ww.mall.promotion.mq.GroupSuccessMessage;
import com.ww.mall.promotion.service.group.GroupInstanceService;
import com.ww.mall.promotion.service.group.convert.GroupConvert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.ww.mall.promotion.config.LuaScriptConfiguration.CREATE_GROUP_SCRIPT_NAME;
import static com.ww.mall.promotion.config.LuaScriptConfiguration.JOIN_GROUP_SCRIPT_NAME;

/**
 * @author ww
 * @create 2025-12-08 17:45
 * @description: 拼团实例服务实现
 */
@Slf4j
@Service
public class GroupInstanceServiceImpl implements GroupInstanceService {

    @Resource
    private LoadingCache<String, GroupActivity> groupActivityCache;

    @Resource
    private RedisScriptComponent redisScriptComponent;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private GroupRedisKeyBuilder groupRedisKeyBuilder;

    @Resource
    private RabbitMqPublisher rabbitMqPublisher;

    private ExecutorService groupAsyncExecutor;

    @PostConstruct
    public void init() {
        // 初始化线程池：核心线程数5，最大线程数10，队列容量100
        groupAsyncExecutor = ThreadUtil.initThreadPoolExecutor("group-async-", 5, 10, 60, TimeUnit.SECONDS, 100, new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupInstanceVO createGroup(CreateGroupRequest request) {
        // 1. 参数校验：订单号必填
        if (request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
            throw new ApiException("订单号不能为空");
        }

        // 2. 查询活动信息
        GroupActivity activity = groupActivityCache.get(request.getActivityId());

        // 3. 校验活动状态
        validateActivity(activity);

        // 4. 检查用户限购
        checkUserLimit(request.getActivityId(), request.getUserId(), activity.getLimitPerUser());

        // 5. 检查库存
        String stockKey = groupRedisKeyBuilder.buildActivityStockKey(request.getActivityId());
        Long stock = stringRedisTemplate.opsForValue().decrement(stockKey);
        if (stock == null || stock < 0) {
            stringRedisTemplate.opsForValue().increment(stockKey);
            throw new ApiException("库存不足");
        }

        // 6. 生成拼团ID
        String groupId = generateGroupId();

        // 7. 构建Redis Key
        String metaKey = groupRedisKeyBuilder.buildGroupMetaKey(groupId);
        String slotsKey = groupRedisKeyBuilder.buildGroupSlotsKey(groupId);
        String membersKey = groupRedisKeyBuilder.buildGroupMembersKey(groupId);
        String ordersKey = groupRedisKeyBuilder.buildGroupOrdersKey(groupId);
        String expiryIndexKey = groupRedisKeyBuilder.buildExpiryIndexKey();

        // 8. 计算过期时间（Lua脚本内部会获取当前时间，这里只需要计算过期时间）
        long expireMillis = System.currentTimeMillis() + activity.getExpireHours() * 3600 * 1000L;

        // 9. 执行Lua脚本创建拼团（Lua脚本内部获取当前时间，减少参数传递）
        List<String> keys = Arrays.asList(metaKey, slotsKey, membersKey, ordersKey);
        List<String> args = Arrays.asList(
                GroupStatus.OPEN.getCode(), // [1] status
                String.valueOf(activity.getRequiredSize()), // [2] requiredSize
                String.valueOf(expireMillis), // [3] expiresAt
                String.valueOf(request.getUserId()), // [4] leaderUserId
                request.getOrderId(), // [5] leaderOrderId
                request.getOrderInfo() != null ? request.getOrderInfo() : "{}", // [6] leaderOrderJson
                String.valueOf(activity.getRequiredSize() - 1), // [7] slotsAfterLeader
                request.getActivityId() // [8] activityId
        );

        try {
            Long result = redisScriptComponent.executeLuaScript(CREATE_GROUP_SCRIPT_NAME,
                    ReturnType.INTEGER,
                    keys,
                    args
            );
            if (result == null || result != 1) {
                // 回滚库存
                rollbackStock(stockKey);
                if (result != null && result == -1) {
                    throw new ApiException("拼团已存在，请勿重复创建");
                } else if (result != null && result == -2) {
                    throw new ApiException("订单已存在，请勿重复提交");
                }
                throw new ApiException("创建拼团失败");
            }
        } catch (ApiException e) {
            rollbackStock(stockKey);
            throw e;
        } catch (Exception e) {
            // 回滚库存
            rollbackStock(stockKey);
            log.error("创建拼团异常: groupId={}, userId={}, orderId={}", groupId, request.getUserId(), request.getOrderId(), e);
            // 发送退款消息
            sendRefundMessage(request.getUserId(), request.getOrderId(), activity.getGroupPrice(), "创建拼团异常");
            throw new ApiException("创建拼团失败: " + e.getMessage());
        }

        // 10. 添加到过期索引
        stringRedisTemplate.opsForZSet().add(expiryIndexKey, groupId, expireMillis);

        // 11. 将用户添加到用户拼团Set中
        String userGroupKey = groupRedisKeyBuilder.buildUserGroupKey(request.getUserId());
        stringRedisTemplate.opsForSet().add(userGroupKey, groupId);

        // 12. 更新用户参与活动次数
        String userActivityCountKey = groupRedisKeyBuilder.buildUserActivityCountKey(request.getActivityId());
        stringRedisTemplate.opsForHash().increment(userActivityCountKey, String.valueOf(request.getUserId()), 1);

        // 13. 保存到MongoDB（异步）
        try {
            saveGroupInstanceToMongo(groupId, request, activity, expireMillis);
        } catch (Exception e) {
            log.error("保存拼团实例到MongoDB失败: groupId={}", groupId, e);
            // 这里不抛异常，因为Redis已经创建成功，后续可以通过定时任务同步
        }

        // 14. 返回结果
        return getGroupDetail(groupId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupInstanceVO joinGroup(JoinGroupRequest request) {
        // 1. 参数校验：订单号必填
        if (request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
            throw new ApiException("订单号不能为空");
        }

        // 2. 从Redis获取拼团信息
        String metaKey = groupRedisKeyBuilder.buildGroupMetaKey(request.getGroupId());
        Map<Object, Object> meta = stringRedisTemplate.opsForHash().entries(metaKey);
        if (meta.isEmpty()) {
            throw new ApiException("拼团不存在或已过期");
        }

        // 3. 获取活动信息（用于退款和限购检查）
        String activityId = String.valueOf(meta.get("activityId"));
        GroupActivity activity = groupActivityCache.get(activityId);

        // 4. 检查用户限购
        checkUserLimit(activityId, request.getUserId(), activity.getLimitPerUser());

        // 5. 构建Redis Key
        String slotsKey = groupRedisKeyBuilder.buildGroupSlotsKey(request.getGroupId());
        String membersKey = groupRedisKeyBuilder.buildGroupMembersKey(request.getGroupId());
        String ordersKey = groupRedisKeyBuilder.buildGroupOrdersKey(request.getGroupId());
        String expiryIndexKey = groupRedisKeyBuilder.buildExpiryIndexKey();

        // 6. 执行Lua脚本加入拼团（Lua脚本内部获取当前时间，减少参数传递）
        List<String> keys = Arrays.asList(metaKey, slotsKey, membersKey, ordersKey, expiryIndexKey);
        List<String> args = Arrays.asList(
                String.valueOf(request.getUserId()), // [1] userId
                request.getOrderId(), // [2] orderId
                request.getOrderInfo() != null ? request.getOrderInfo() : "{}", // [3] orderJson
                request.getGroupId() // [4] groupId
        );

        try {
            Long result = redisScriptComponent.executeLuaScript(JOIN_GROUP_SCRIPT_NAME,
                    ReturnType.INTEGER,
                    keys,
                    args
            );
            if (result == null) {
                throw new ApiException("拼团异常");
            }
            String errorMsg = "加入拼团失败";
            if (result <= 0) {
                if (result == -1) {
                    errorMsg = "拼团不存在或已过期";
                } else if (result == -2) {
                    errorMsg = "拼团未开放";
                } else if (result == -3) {
                    errorMsg = "拼团已过期";
                } else if (result == -4) {
                    errorMsg = "您已在此拼团中";
                } else if (result == -5) {
                    errorMsg = "订单已存在，请勿重复提交";
                } else if (result == -6) {
                    errorMsg = "拼团已满，没有剩余名额";
                }
                throw new ApiException(errorMsg);
            }

            // 检查是否拼团成功
            if (result == 1) {
                // 拼团成功，异步处理
                handleGroupSuccessAsync(request.getGroupId());
            } else if (result == 2) {
                // 加入成功，将用户添加到用户拼团Set中
                String userGroupKey = groupRedisKeyBuilder.buildUserGroupKey(request.getUserId());
                stringRedisTemplate.opsForSet().add(userGroupKey, request.getGroupId());
                // 更新用户参与活动次数
                String userActivityCountKey = groupRedisKeyBuilder.buildUserActivityCountKey(activityId);
                stringRedisTemplate.opsForHash().increment(userActivityCountKey, String.valueOf(request.getUserId()), 1);
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("加入拼团异常: groupId={}, userId={}, orderId={}", 
                    request.getGroupId(), request.getUserId(), request.getOrderId(), e);
            // 发送退款消息
            sendRefundMessage(request.getUserId(), request.getOrderId(), activity.getGroupPrice(), "加入拼团异常");
            throw new ApiException("加入拼团失败: " + e.getMessage());
        }

        // 7. 保存成员信息到MongoDB（异步）
        try {
            saveGroupMemberToMongo(request.getGroupId(), request, activity);
        } catch (Exception e) {
            log.error("保存拼团成员到MongoDB失败: groupId={}, userId={}", request.getGroupId(), request.getUserId(), e);
        }

        // 8. 返回结果
        return getGroupDetail(request.getGroupId());
    }

    @Override
    public GroupInstanceVO getGroupDetail(String groupId) {
        // 1. 从Redis获取拼团信息
        String metaKey = groupRedisKeyBuilder.buildGroupMetaKey(groupId);
        Map<Object, Object> meta = stringRedisTemplate.opsForHash().entries(metaKey);
        if (meta.isEmpty()) {
            // 从MongoDB获取
            GroupInstance instance = mongoTemplate.findOne(
                    GroupInstance.buildIdQuery(groupId), GroupInstance.class);
            if (instance == null) {
                throw new ApiException("拼团不存在");
            }
            return convertToVO(instance);
        }

        // 2. 构建VO
        GroupInstanceVO vo = new GroupInstanceVO();
        vo.setId(groupId);
        vo.setStatus(String.valueOf(meta.get("status")));
        vo.setRequiredSize(Integer.valueOf(String.valueOf(meta.get("requiredSize"))));
        vo.setCurrentSize(Integer.valueOf(String.valueOf(meta.getOrDefault("currentSize", "1"))));
        
        // 3. 获取剩余名额
        String slotsKey = groupRedisKeyBuilder.buildGroupSlotsKey(groupId);
        String slotsStr = stringRedisTemplate.opsForValue().get(slotsKey);
        vo.setRemainingSlots(slotsStr != null ? Integer.parseInt(slotsStr) : 0);

        // 4. 获取成员列表和订单信息
        String membersKey = groupRedisKeyBuilder.buildGroupMembersKey(groupId);
        Set<String> memberIds = stringRedisTemplate.opsForZSet().range(membersKey, 0, -1);
        if (memberIds != null && !memberIds.isEmpty()) {
            List<GroupInstanceVO.MemberInfo> members = new ArrayList<>();
            Long leaderUserId = Long.valueOf(String.valueOf(meta.get("leaderUserId")));
            
            // 从MongoDB批量查询成员订单信息（更准确）
            Query memberQuery = GroupMember.buildGroupInstanceIdQuery(groupId);
            List<GroupMember> memberList = mongoTemplate.find(memberQuery, GroupMember.class);
            Map<Long, GroupMember> memberMap = memberList.stream()
                    .collect(Collectors.toMap(GroupMember::getUserId, m -> m));
            
            for (String userId : memberIds) {
                GroupInstanceVO.MemberInfo member = new GroupInstanceVO.MemberInfo();
                Long uid = Long.valueOf(userId);
                member.setUserId(uid);
                member.setIsLeader(uid.equals(leaderUserId));
                
                // 从MongoDB获取订单信息
                GroupMember memberInfo = memberMap.get(uid);
                if (memberInfo != null) {
                    member.setOrderId(memberInfo.getOrderId());
                    member.setJoinTime(memberInfo.getJoinTime());
                } else {
                    // 如果MongoDB中没有，从Redis的Sorted Set获取加入时间
                    Double score = stringRedisTemplate.opsForZSet().score(membersKey, userId);
                    if (score != null) {
                        member.setJoinTime(new Date(score.longValue()));
                    }
                }
                
                members.add(member);
            }
            vo.setMembers(members);
        }

        return vo;
    }

    @Override
    public List<GroupInstanceVO> getUserGroups(Long userId) {
        String userGroupKey = groupRedisKeyBuilder.buildUserGroupKey(userId);
        Set<String> groupIds = stringRedisTemplate.opsForSet().members(userGroupKey);
        if (groupIds == null || groupIds.isEmpty()) {
            // 从MongoDB查询
            Query query = GroupMember.buildUserIdAndStatusQuery(userId, GroupMemberStatus.NORMAL.getCode());
            List<GroupMember> members = mongoTemplate.find(query, GroupMember.class);
            return members.stream()
                    .map(m -> getGroupDetail(m.getGroupInstanceId()))
                    .collect(Collectors.toList());
        }
        return groupIds.stream()
                .map(this::getGroupDetail)
                .collect(Collectors.toList());
    }

    @Override
    public List<GroupInstanceVO> getActivityGroups(String activityId, String status) {
        Query query = GroupInstance.buildActivityIdAndStatusQuery(activityId, status);
        List<GroupInstance> instances = mongoTemplate.find(query, GroupInstance.class);
        return instances.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public void handleGroupSuccess(String groupId) {
        log.info("处理拼团成功: groupId={}", groupId);
        // 更新Redis状态
        String metaKey = groupRedisKeyBuilder.buildGroupMetaKey(groupId);
        stringRedisTemplate.opsForHash().put(metaKey, "status", GroupStatus.SUCCESS.getCode());
        stringRedisTemplate.opsForHash().put(metaKey, "completeTime", String.valueOf(System.currentTimeMillis()));

        // 更新MongoDB
        Query query = GroupInstance.buildIdQuery(groupId);
        mongoTemplate.updateFirst(query, GroupInstance.buildStatusUpdate(GroupStatus.SUCCESS.getCode()), GroupInstance.class);

        // 发送消息到RabbitMQ（异步通知订单系统等）
        sendGroupSuccessMessage(groupId);
    }

    @Override
    public void handleGroupFailed(String groupId) {
        log.info("处理拼团失败: groupId={}", groupId);
        
        // 1. 更新Redis状态
        String metaKey = groupRedisKeyBuilder.buildGroupMetaKey(groupId);
        stringRedisTemplate.opsForHash().put(metaKey, "status", GroupStatus.FAILED.getCode());
        stringRedisTemplate.opsForHash().put(metaKey, "failedTime", String.valueOf(System.currentTimeMillis()));

        // 2. 更新MongoDB
        Query query = GroupInstance.buildIdQuery(groupId);
        mongoTemplate.updateFirst(query, GroupInstance.buildStatusUpdate(GroupStatus.FAILED.getCode()), GroupInstance.class);

        // 3. 获取拼团成员信息，发送退款消息
        Query memberQuery = GroupMember.buildGroupInstanceIdQuery(groupId);
        List<GroupMember> members = mongoTemplate.find(memberQuery, GroupMember.class);
        if (CollectionUtil.isNotEmpty(members)) {
            sendGroupRefundMessage(groupId, members, "拼团失败");
        }
    }

    /**
     * 校验活动
     */
    private void validateActivity(GroupActivity activity) {
        Date now = new Date();
        if (activity.getEnabled() == 0) {
            throw new ApiException("活动已禁用");
        }
        if (activity.getStartTime().after(now)) {
            throw new ApiException("活动未开始");
        }
        if (activity.getEndTime().before(now)) {
            throw new ApiException("活动已结束");
        }
    }

    /**
     * 检查用户限购
     */
    private void checkUserLimit(String activityId, Long userId, Integer limitPerUser) {
        if (limitPerUser == null || limitPerUser <= 0) {
            // 不限购
            return;
        }

        // 从Redis获取用户参与次数
        String userActivityCountKey = groupRedisKeyBuilder.buildUserActivityCountKey(activityId);
        Object countObj = stringRedisTemplate.opsForHash().get(userActivityCountKey, String.valueOf(userId));
        int currentCount = countObj != null ? Integer.parseInt(String.valueOf(countObj)) : 0;

        // 如果Redis中没有，从MongoDB查询
        if (currentCount == 0) {
            Query query = GroupMember.buildUserIdAndStatusQuery(userId, GroupMemberStatus.NORMAL.getCode());
            List<GroupMember> members = mongoTemplate.find(query, GroupMember.class);
            // 统计该用户在该活动下的参与次数
            currentCount = (int) members.stream()
                    .filter(m -> activityId.equals(m.getActivityId()))
                    .count();
        }

        if (currentCount >= limitPerUser) {
            throw new ApiException("您已达到该活动的限购数量，每人限购" + limitPerUser + "件");
        }
    }

    /**
     * 生成拼团ID
     */
    private String generateGroupId() {
        return "GROUP_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 回滚库存
     */
    private void rollbackStock(String stockKey) {
        try {
            stringRedisTemplate.opsForValue().increment(stockKey);
        } catch (Exception e) {
            log.error("回滚库存失败: stockKey={}", stockKey, e);
        }
    }

    /**
     * 保存拼团实例到MongoDB
     */
    private void saveGroupInstanceToMongo(String groupId, CreateGroupRequest request, 
                                         GroupActivity activity, long expireMillis) {
        GroupInstance instance = new GroupInstance();
        instance.setId(groupId);
        instance.setActivityId(request.getActivityId());
        instance.setLeaderUserId(request.getUserId());
        instance.setStatus(GroupStatus.OPEN.getCode());
        instance.setRequiredSize(activity.getRequiredSize());
        instance.setCurrentSize(1);
        instance.setRemainingSlots(activity.getRequiredSize() - 1);
        instance.setExpireTime(new Date(expireMillis));
        instance.setGroupPrice(activity.getGroupPrice());
        instance.setSpuId(activity.getSpuId());
        instance.setSkuId(activity.getSkuId());
        mongoTemplate.save(instance);

        // 保存团长成员信息
        GroupMember leader = new GroupMember();
        leader.setGroupInstanceId(groupId);
        leader.setActivityId(request.getActivityId());
        leader.setUserId(request.getUserId());
        leader.setOrderId(request.getOrderId());
        leader.setIsLeader(1);
        leader.setJoinTime(new Date());
        leader.setGroupPrice(activity.getGroupPrice());
        leader.setSpuId(activity.getSpuId());
        leader.setSkuId(activity.getSkuId());
        leader.setStatus(GroupMemberStatus.NORMAL.getCode());
        mongoTemplate.save(leader);
    }

    /**
     * 保存成员信息到MongoDB
     */
    private void saveGroupMemberToMongo(String groupId, JoinGroupRequest request, GroupActivity activity) {
        GroupInstance instance = mongoTemplate.findOne(
                GroupInstance.buildIdQuery(groupId), GroupInstance.class);
        if (instance == null) {
            return;
        }

        GroupMember member = new GroupMember();
        member.setGroupInstanceId(groupId);
        member.setActivityId(instance.getActivityId());
        member.setUserId(request.getUserId());
        member.setOrderId(request.getOrderId());
        member.setIsLeader(0);
        member.setJoinTime(new Date());
        member.setGroupPrice(instance.getGroupPrice());
        member.setSpuId(instance.getSpuId());
        member.setSkuId(instance.getSkuId());
        member.setStatus(GroupMemberStatus.NORMAL.getCode());
        mongoTemplate.save(member);

        // 更新实例当前人数
        instance.setCurrentSize(instance.getCurrentSize() + 1);
        instance.setRemainingSlots(instance.getRemainingSlots() - 1);
        mongoTemplate.save(instance);
    }

    /**
     * 异步处理拼团成功
     */
    private void handleGroupSuccessAsync(String groupId) {
        // 使用线程池异步处理
        groupAsyncExecutor.execute(() -> {
            try {
                handleGroupSuccess(groupId);
            } catch (Exception e) {
                log.error("异步处理拼团成功异常: groupId={}", groupId, e);
            }
        });
    }

    /**
     * 转换为VO
     */
    private GroupInstanceVO convertToVO(GroupInstance instance) {
        return GroupConvert.INSTANCE.groupInstanceToVO( instance);
    }

    /**
     * 发送退款消息
     */
    private void sendRefundMessage(Long userId, String orderId, java.math.BigDecimal amount, String reason) {
        try {
            GroupRefundMessage.RefundOrder refundOrder = new GroupRefundMessage.RefundOrder();
            refundOrder.setUserId(userId);
            refundOrder.setOrderId(orderId);
            refundOrder.setRefundAmount(amount);
            refundOrder.setIsLeader(false);

            GroupRefundMessage message = new GroupRefundMessage();
            message.setReason(reason);
            message.setRefundOrders(Collections.singletonList(refundOrder));

            rabbitMqPublisher.sendMsg(GroupMqConstant.GROUP_EXCHANGE, GroupMqConstant.GROUP_REFUND_KEY, message);
            log.info("发送退款消息: userId={}, orderId={}, reason={}", userId, orderId, reason);
        } catch (Exception e) {
            log.error("发送退款消息失败: userId={}, orderId={}", userId, orderId, e);
        }
    }


    /**
     * 发送拼团成功消息
     */
    private void sendGroupSuccessMessage(String groupId) {
        try {
            // 获取拼团信息
            GroupInstance instance = mongoTemplate.findOne(
                    GroupInstance.buildIdQuery(groupId), GroupInstance.class);
            if (instance == null) {
                log.warn("拼团实例不存在，无法发送成功消息: groupId={}", groupId);
                return;
            }

            // 获取成员订单信息
            Query memberQuery = GroupMember.buildGroupInstanceIdQuery(groupId);
            List<GroupMember> members = mongoTemplate.find(memberQuery, GroupMember.class);
            
            List<GroupSuccessMessage.MemberOrder> memberOrders = members.stream()
                    .map(member -> {
                        GroupSuccessMessage.MemberOrder order = new GroupSuccessMessage.MemberOrder();
                        order.setUserId(member.getUserId());
                        order.setOrderId(member.getOrderId());
                        order.setIsLeader(member.getIsLeader() == 1);
                        return order;
                    })
                    .collect(Collectors.toList());

            GroupSuccessMessage message = new GroupSuccessMessage();
            message.setGroupId(groupId);
            message.setActivityId(instance.getActivityId());
            message.setCompleteTime(instance.getCompleteTime());
            message.setMemberOrders(memberOrders);

            rabbitMqPublisher.sendMsg(GroupMqConstant.GROUP_EXCHANGE, GroupMqConstant.GROUP_SUCCESS_KEY, message);
            log.info("发送拼团成功消息: groupId={}, memberCount={}", groupId, memberOrders.size());
        } catch (Exception e) {
            log.error("发送拼团成功消息失败: groupId={}", groupId, e);
        }
    }

    /**
     * 发送拼团退款消息
     */
    private void sendGroupRefundMessage(String groupId, List<GroupMember> members, String reason) {
        try {
            GroupInstance instance = mongoTemplate.findOne(
                    GroupInstance.buildIdQuery(groupId), GroupInstance.class);
            if (instance == null) {
                return;
            }

            List<GroupRefundMessage.RefundOrder> refundOrders = members.stream()
                    .map(member -> {
                        GroupRefundMessage.RefundOrder order = new GroupRefundMessage.RefundOrder();
                        order.setUserId(member.getUserId());
                        order.setOrderId(member.getOrderId());
                        order.setRefundAmount(member.getGroupPrice());
                        order.setIsLeader(member.getIsLeader() == 1);
                        return order;
                    })
                    .collect(Collectors.toList());

            GroupRefundMessage message = new GroupRefundMessage();
            message.setGroupId(groupId);
            message.setActivityId(instance.getActivityId());
            message.setReason(reason);
            message.setRefundOrders(refundOrders);

            rabbitMqPublisher.sendMsg(GroupMqConstant.GROUP_EXCHANGE, GroupMqConstant.GROUP_REFUND_KEY, message);
            log.info("发送拼团退款消息: groupId={}, memberCount={}, reason={}", groupId, members.size(), reason);
        } catch (Exception e) {
            log.error("发送拼团退款消息失败: groupId={}", groupId, e);
        }
    }

}
