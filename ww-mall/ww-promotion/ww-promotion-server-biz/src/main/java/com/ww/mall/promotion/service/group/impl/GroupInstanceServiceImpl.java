package com.ww.mall.promotion.service.group.impl;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.app.common.common.ResCode;
import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.common.exception.ApiException;
import com.ww.app.disruptor.api.DisruptorTemplate;
import com.ww.app.disruptor.model.Event;
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
import com.ww.mall.promotion.event.GroupEvent;
import com.ww.mall.promotion.key.GroupRedisKeyBuilder;
import com.ww.mall.promotion.mq.GroupMqConstant;
import com.ww.mall.promotion.mq.GroupRefundMessage;
import com.ww.mall.promotion.service.group.GroupInstanceService;
import com.ww.mall.promotion.service.group.convert.GroupConvert;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.ww.mall.promotion.config.LuaScriptConfiguration.CREATE_GROUP_SCRIPT_NAME;
import static com.ww.mall.promotion.config.LuaScriptConfiguration.JOIN_GROUP_SCRIPT_NAME;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.*;

/**
 * @author ww
 * @create 2025-12-08 17:45
 * @description: 拼团实例服务实现（优化版）
 * 优化点：
 * 1. 减少Redis往返次数：将库存扣减、用户计数等操作合并到Lua脚本中
 * 2. 使用Disruptor替换线程池：提高异步处理性能和可靠性
 * 3. 增强错误追踪：记录错误信息到Redis，便于排查问题
 * 4. 保证业务完整性：即使异常也不影响主流程，数据可追溯
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
    private DisruptorTemplate<GroupEvent> groupDisruptorTemplate;

    @Resource
    private RabbitMqPublisher rabbitMqPublisher;

    @Override
    public GroupInstanceVO createGroup(CreateGroupRequest request) {
        // 1. 参数校验
        if (request == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        if (request.getActivityId() == null || request.getActivityId().trim().isEmpty()) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        if (request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
            throw new ApiException(GROUP_RECORD_ORDER_CODE_NOT_EXISTS);
        }

        // 2. 查询活动信息
        GroupActivity activity = groupActivityCache.get(request.getActivityId());

        // 3. 校验活动状态
        validateActivity(activity);

        // 4. 检查用户限购
        Long userId = AuthorizationContext.getClientUser().getId();
        checkUserLimit(request.getActivityId(), userId, activity.getLimitPerUser());

        // 5. 生成拼团ID
        String groupId = new ObjectId().toString();

        // 6. 构建Redis Key（包含库存Key和错误追踪Key）
        String metaKey = groupRedisKeyBuilder.buildGroupMetaKey(groupId);
        String slotsKey = groupRedisKeyBuilder.buildGroupSlotsKey(groupId);
        String membersKey = groupRedisKeyBuilder.buildGroupMembersKey(groupId);
        String ordersKey = groupRedisKeyBuilder.buildGroupOrdersKey(groupId);
        String expiryIndexKey = groupRedisKeyBuilder.buildExpiryIndexKey();
        String userGroupKey = groupRedisKeyBuilder.buildUserGroupKey(userId);
        String userCountKey = groupRedisKeyBuilder.buildUserActivityCountKey(request.getActivityId());
        String stockKey = groupRedisKeyBuilder.buildActivityStockKey(request.getActivityId());

        // 7. 计算过期时间
        long expireMillis = System.currentTimeMillis() + activity.getExpireHours() * 3600 * 1000L;

        // 8. 执行Lua脚本创建拼团（包含库存扣减、用户计数等操作，一次完成）
        List<String> keys = Arrays.asList(
                metaKey, slotsKey, membersKey, ordersKey,
                userGroupKey, userCountKey, expiryIndexKey,
                stockKey
        );
        List<String> args = Arrays.asList(
                GroupStatus.OPEN.getCode(), // [1] status
                String.valueOf(activity.getRequiredSize()), // [2] requiredSize
                String.valueOf(expireMillis), // [3] expiresAt
                String.valueOf(userId), // [4] leaderUserId
                request.getOrderId(), // [5] leaderOrderId
                request.getOrderInfo() != null ? request.getOrderInfo() : "{}", // [6] leaderOrderJson
                String.valueOf(activity.getRequiredSize() - 1), // [7] slotsAfterLeader
                request.getActivityId(), // [8] activityId
                groupId // [9] groupId
        );

        try {
            Long result = redisScriptComponent.executeLuaScript(CREATE_GROUP_SCRIPT_NAME,
                    ReturnType.INTEGER,
                    keys,
                    args
            );

            if (result == null || result != 1) {
                // 处理错误情况
                handleCreateGroupError(result, userId, request.getOrderId(), activity.getGroupPrice());
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建拼团异常: groupId={}, userId={}, orderId={}", groupId, userId, request.getOrderId(), e);
            // 发送退款消息
            sendRefundMessage(userId, request.getOrderId(), activity.getGroupPrice(), "创建拼团异常");
            throw new ApiException(GROUP_CREATE_FAILED.getMsg() + ": " + e.getMessage());
        }

        // 9. 异步保存到MongoDB（使用Disruptor）
        publishSaveInstanceEvent(groupId, request, activity, expireMillis, userId);

        // 10. 返回结果
        return getGroupDetail(groupId);
    }

    @Override
    public GroupInstanceVO joinGroup(JoinGroupRequest request) {
        // 1. 参数校验
        if (request == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        if (request.getGroupId() == null || request.getGroupId().trim().isEmpty()) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        if (request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
            throw new ApiException(GROUP_RECORD_ORDER_CODE_NOT_EXISTS);
        }

        // 2. 从Redis获取拼团信息（仅用于获取活动ID和校验）
        String metaKey = groupRedisKeyBuilder.buildGroupMetaKey(request.getGroupId());
        Map<Object, Object> meta = stringRedisTemplate.opsForHash().entries(metaKey);
        if (meta.isEmpty()) {
            throw new ApiException(GROUP_RECORD_NOT_EXISTS);
        }

        // 3. 获取活动信息（用于退款和限购检查）
        Object activityIdObj = meta.get("activityId");
        if (activityIdObj == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        String activityId = String.valueOf(activityIdObj);
        GroupActivity activity = groupActivityCache.get(activityId);
        if (activity == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }

        // 4. 检查用户限购
        Long userId = AuthorizationContext.getClientUser().getId();
        checkUserLimit(activityId, userId, activity.getLimitPerUser());

        // 5. 构建Redis Key（包含错误追踪Key）
        String slotsKey = groupRedisKeyBuilder.buildGroupSlotsKey(request.getGroupId());
        String membersKey = groupRedisKeyBuilder.buildGroupMembersKey(request.getGroupId());
        String ordersKey = groupRedisKeyBuilder.buildGroupOrdersKey(request.getGroupId());
        String expiryIndexKey = groupRedisKeyBuilder.buildExpiryIndexKey();
        String userGroupKey = groupRedisKeyBuilder.buildUserGroupKey(userId);
        String userCountKey = groupRedisKeyBuilder.buildUserActivityCountKey(activityId);

        // 6. 执行Lua脚本加入拼团（包含用户组Set、用户计数等操作，一次完成）
        List<String> keys = Arrays.asList(
                metaKey, slotsKey, membersKey, ordersKey,
                expiryIndexKey, userGroupKey, userCountKey
        );
        List<String> args = Arrays.asList(
                String.valueOf(userId), // [1] userId
                request.getOrderId(), // [2] orderId
                request.getOrderInfo() != null ? request.getOrderInfo() : "{}", // [3] orderJson
                request.getGroupId() // [4] groupId
        );

        try {
            Object luaResult = redisScriptComponent.executeLuaScript(JOIN_GROUP_SCRIPT_NAME,
                    ReturnType.MULTI,
                    keys,
                    args
            );

            if (!(luaResult instanceof List)) {
                throw new ApiException(GROUP_RECORD_ERROR);
            }

            List<?> res = (List<?>) luaResult;
            Long code = !res.isEmpty() && res.get(0) instanceof Long ? (Long) res.get(0) : null;
            Long newSlots = res.size() > 1 && res.get(1) instanceof Long ? (Long) res.get(1) : null;
            Long completeTime = res.size() > 2 && res.get(2) instanceof Long ? (Long) res.get(2) : null;

            if (code == null || code <= 0) {
                handleJoinGroupError(code, userId, request.getOrderId(), activity.getGroupPrice());
                throw new ApiException(GROUP_RECORD_ERROR);
            }

            if (code == 1) {
                // 拼团成功，携带完成时间减少后查
                publishGroupSuccessEvent(request.getGroupId(), completeTime);
            } else if (code == 2) {
                // 加入成功，异步保存成员信息到MongoDB，携带剩余名额
                publishSaveMemberEvent(request.getGroupId(), request, activityId, newSlots);
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("加入拼团异常: groupId={}, userId={}, orderId={}",
                    request.getGroupId(), userId, request.getOrderId(), e);
            // 发送退款消息
            sendRefundMessage(userId, request.getOrderId(), activity.getGroupPrice(), "加入拼团异常");
            throw new ApiException(GROUP_RECORD_ERROR.getMsg() + ": " + e.getMessage());
        }

        // 7. 返回结果
        return getGroupDetail(request.getGroupId());
    }

    @Override
    public GroupInstanceVO getGroupDetail(String groupId) {
        // 参数校验
        if (groupId == null || groupId.trim().isEmpty()) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }

        // 1. 从Redis获取拼团信息
        String metaKey = groupRedisKeyBuilder.buildGroupMetaKey(groupId);
        Map<Object, Object> meta = stringRedisTemplate.opsForHash().entries(metaKey);
        if (meta.isEmpty()) {
            // 从MongoDB获取
            GroupInstance instance = mongoTemplate.findOne(
                    GroupInstance.buildIdQuery(groupId), GroupInstance.class);
            if (instance == null) {
                throw new ApiException(GROUP_RECORD_NOT_EXISTS);
            }
            return convertToVO(instance);
        }

            // 2. 构建VO
        GroupInstanceVO vo = new GroupInstanceVO();
        vo.setId(groupId);
        vo.setStatus(getStringValue(meta.get("status"), GroupStatus.OPEN.getCode()));
        vo.setRequiredSize(getIntegerValue(meta.get("requiredSize"), 1));
        vo.setCurrentSize(getIntegerValue(meta.get("currentSize"), 1));

        // 3. 获取剩余名额
        String slotsKey = groupRedisKeyBuilder.buildGroupSlotsKey(groupId);
        String slotsStr = stringRedisTemplate.opsForValue().get(slotsKey);
        vo.setRemainingSlots(slotsStr != null ? Integer.parseInt(slotsStr) : 0);

        // 4. 获取成员列表和订单信息
        String membersKey = groupRedisKeyBuilder.buildGroupMembersKey(groupId);
        Set<String> memberIds = stringRedisTemplate.opsForZSet().range(membersKey, 0, -1);
        if (memberIds != null && !memberIds.isEmpty()) {
            List<GroupInstanceVO.MemberInfo> members = new ArrayList<>();
            Object leaderUserIdObj = meta.get("leaderUserId");
            if (leaderUserIdObj == null) {
                log.warn("拼团缺少团长ID: groupId={}", groupId);
                return vo;
            }
            Long leaderUserId = Long.valueOf(String.valueOf(leaderUserIdObj));

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
    public List<GroupInstanceVO> getUserGroups() {
        Long userId = AuthorizationContext.getClientUser().getId();
        if (userId == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
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
        if (activityId == null || activityId.trim().isEmpty()) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        Query query = GroupInstance.buildActivityIdAndStatusQuery(activityId, status);
        List<GroupInstance> instances = mongoTemplate.find(query, GroupInstance.class);
        return instances.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public void handleGroupSuccess(String groupId) {
        log.info("处理拼团成功: groupId={}", groupId);
        publishGroupSuccessEvent(groupId);
    }

    @Override
    public void handleGroupFailed(String groupId) {
        log.info("处理拼团失败: groupId={}", groupId);
        publishGroupFailedEvent(groupId, GROUP_RECORD_ERROR.getMsg());
    }

    /**
     * 校验活动
     */
    private void validateActivity(GroupActivity activity) {
        Date now = new Date();
        if (activity.getEnabled() == 0) {
            throw new ApiException(GROUP_RECORD_FAILED_DISABLE);
        }
        if (activity.getStartTime().after(now)) {
            throw new ApiException(GROUP_RECORD_FAILED_TIME_NOT_START);
        }
        if (activity.getEndTime().before(now)) {
            throw new ApiException(GROUP_RECORD_FAILED_TIME_END);
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
        int currentCount = 0;
        if (countObj != null) {
            try {
                currentCount = Integer.parseInt(String.valueOf(countObj));
            } catch (NumberFormatException e) {
                log.warn("解析用户参与次数失败: activityId={}, userId={}, countObj={}", activityId, userId, countObj, e);
            }
        }

        // 如果Redis中没有或为0，从MongoDB查询（作为兜底，保证数据准确性）
        if (currentCount == 0) {
            Query query = GroupMember.buildUserIdAndStatusQuery(userId, GroupMemberStatus.NORMAL.getCode());
            List<GroupMember> members = mongoTemplate.find(query, GroupMember.class);
            if (!members.isEmpty()) {
                // 统计该用户在该活动下的参与次数
                currentCount = (int) members.stream()
                        .filter(m -> activityId.equals(m.getActivityId()))
                        .count();
            }
        }

        if (currentCount >= limitPerUser) {
            throw new ApiException(GROUP_RECORD_FAILED_TOTAL_LIMIT_COUNT_EXCEED);
        }
    }

    /**
     * 处理创建拼团错误
     */
    private void handleCreateGroupError(Long result, Long userId, String orderId, BigDecimal groupPrice) {
        String errorMsg;
        ResCode errorCode;
        if (result == null) {
            errorMsg = GROUP_CREATE_FAILED.getMsg();
            errorCode = GROUP_CREATE_FAILED;
        } else if (result == -1) {
            errorMsg = GROUP_RECORD_FAILED_HAVE_JOINED.getMsg();
            errorCode = GROUP_RECORD_FAILED_HAVE_JOINED;
        } else if (result == -2) {
            errorMsg = GROUP_RECORD_EXISTS.getMsg();
            errorCode = GROUP_RECORD_EXISTS;
        } else if (result == -3) {
            errorMsg = GROUP_RECORD_STOCK_NOT_ENOUGH.getMsg();
            errorCode = GROUP_RECORD_STOCK_NOT_ENOUGH;
        } else {
            errorMsg = GROUP_CREATE_FAILED.getMsg();
            errorCode = GROUP_CREATE_FAILED;
        }

        sendRefundMessage(userId, orderId, groupPrice, errorMsg);
        throw new ApiException(errorCode);
    }

    /**
     * 处理加入拼团错误
     */
    private void handleJoinGroupError(Long result, Long userId, String orderId, BigDecimal groupPrice) {
        String errorMsg;
        ResCode errorCode;
        if (result == -1) {
            errorMsg = GROUP_RECORD_NOT_EXISTS.getMsg();
            errorCode = GROUP_RECORD_NOT_EXISTS;
        } else if (result == -2) {
            errorMsg = GROUP_RECORD_FAILED_TIME_NOT_START.getMsg();
            errorCode = GROUP_RECORD_FAILED_TIME_NOT_START;
        } else if (result == -3) {
            errorMsg = GROUP_RECORD_FAILED_TIME_END.getMsg();
            errorCode = GROUP_RECORD_FAILED_TIME_END;
        } else if (result == -4) {
            errorMsg = GROUP_RECORD_EXISTS.getMsg();
            errorCode = GROUP_RECORD_EXISTS;
        } else if (result == -5) {
            errorMsg = GROUP_RECORD_FAILED_ORDER_STATUS_UNPAID.getMsg();
            errorCode = GROUP_RECORD_FAILED_ORDER_STATUS_UNPAID;
        } else if (result == -6) {
            errorMsg = GROUP_RECORD_USER_FULL.getMsg();
            errorCode = GROUP_RECORD_USER_FULL;
        } else {
            errorMsg = GROUP_RECORD_ERROR.getMsg();
            errorCode = GROUP_RECORD_ERROR;
        }

        sendRefundMessage(userId, orderId, groupPrice, errorMsg);
        throw new ApiException(errorCode);
    }

    /**
     * 发送退款消息
     */
    private void sendRefundMessage(Long userId, String orderId, BigDecimal amount, String reason) {
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
     * 发布保存拼团实例事件
     */
    private void publishSaveInstanceEvent(String groupId, CreateGroupRequest request,
                                          GroupActivity activity, long expireMillis, Long userId) {
        try {
            GroupEvent event = new GroupEvent(GroupEvent.EventType.SAVE_INSTANCE, groupId);
            event.setActivityId(request.getActivityId());
            event.setUserId(userId);
            event.setOrderId(request.getOrderId());
            event.setOrderInfo(request.getOrderInfo());
            event.addExtInfo("groupPrice", activity.getGroupPrice());
            event.addExtInfo("spuId", activity.getSpuId());
            event.addExtInfo("skuId", activity.getSkuId());
            event.addExtInfo("expireMillis", expireMillis);
            event.addExtInfo("requiredSize", activity.getRequiredSize());
            event.addExtInfo("currentSize", 1);
            event.addExtInfo("remainingSlots", activity.getRequiredSize() - 1);
            event.addExtInfo("status", GroupStatus.OPEN.getCode());
            event.addExtInfo("leaderUserId", userId);
            event.addExtInfo("createdAt", System.currentTimeMillis());

            boolean published = groupDisruptorTemplate.publish(new Event<>("SAVE_INSTANCE", event));
            if (!published) {
                log.warn("发布保存实例事件失败: groupId={}", groupId);
            }
        } catch (Exception e) {
            log.error("发布保存实例事件异常: groupId={}", groupId, e);
            // 不抛异常，不影响主流程
        }
    }

    /**
     * 发布保存成员事件
     */
    private void publishSaveMemberEvent(String groupId, JoinGroupRequest request, String activityId, Long newSlots) {
        try {
            GroupEvent event = new GroupEvent(GroupEvent.EventType.SAVE_MEMBER, groupId);
            event.setActivityId(activityId);
            event.setUserId(AuthorizationContext.getClientUser().getId());
            event.setOrderId(request.getOrderId());
            event.setOrderInfo(request.getOrderInfo());
            GroupActivity activity = groupActivityCache.get(activityId);
            if (activity != null) {
                event.addExtInfo("groupPrice", activity.getGroupPrice());
                event.addExtInfo("spuId", activity.getSpuId());
                event.addExtInfo("skuId", activity.getSkuId());
            }
            event.addExtInfo("joinTime", System.currentTimeMillis());
            if (newSlots != null) {
                event.addExtInfo("newSlots", newSlots);
            }

            boolean published = groupDisruptorTemplate.publish(new Event<>("SAVE_MEMBER", event));
            if (!published) {
                log.warn("发布保存成员事件失败: groupId={}", groupId);
            }
        } catch (Exception e) {
            log.error("发布保存成员事件异常: groupId={}", groupId, e);
            // 不抛异常，不影响主流程
        }
    }

    /**
     * 发布拼团成功事件
     */
    private void publishGroupSuccessEvent(String groupId) {
        publishGroupSuccessEvent(groupId, null);
    }

    private void publishGroupSuccessEvent(String groupId, Long completeTime) {
        try {
            GroupEvent event = new GroupEvent(GroupEvent.EventType.GROUP_SUCCESS, groupId);
            if (completeTime != null) {
                event.addExtInfo("completeTime", completeTime);
            }
            boolean published = groupDisruptorTemplate.publish(new Event<>("GROUP_SUCCESS", event));
            if (!published) {
                log.warn("发布拼团成功事件失败: groupId={}", groupId);
            }
        } catch (Exception e) {
            log.error("发布拼团成功事件异常: groupId={}", groupId, e);
            // 不抛异常，不影响主流程
        }
    }

    /**
     * 发布拼团失败事件
     */
    private void publishGroupFailedEvent(String groupId, String reason) {
        try {
            GroupEvent event = new GroupEvent(GroupEvent.EventType.GROUP_FAILED, groupId);
            event.setErrorMessage(reason);
            boolean published = groupDisruptorTemplate.publish(new Event<>("GROUP_FAILED", event));
            if (!published) {
                log.warn("发布拼团失败事件失败: groupId={}", groupId);
            }
        } catch (Exception e) {
            log.error("发布拼团失败事件异常: groupId={}", groupId, e);
            // 不抛异常，不影响主流程
        }
    }

    /**
     * 转换为VO
     */
    private GroupInstanceVO convertToVO(GroupInstance instance) {
        return GroupConvert.INSTANCE.groupInstanceToVO(instance);
    }

    /**
     * 安全获取整数值
     */
    private Integer getIntegerValue(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            log.warn("转换整数值失败: value={}, defaultValue={}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 安全获取字符串值
     */
    private String getStringValue(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return String.valueOf(value);
    }
}
