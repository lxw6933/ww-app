package com.ww.mall.promotion.processor;

import cn.hutool.core.collection.CollectionUtil;
import com.ww.app.disruptor.model.Event;
import com.ww.app.disruptor.model.EventBatch;
import com.ww.app.disruptor.model.ProcessResult;
import com.ww.app.disruptor.processor.BatchEventProcessor;
import com.ww.app.rabbitmq.RabbitMqPublisher;
import com.ww.mall.promotion.entity.group.GroupInstance;
import com.ww.mall.promotion.entity.group.GroupMember;
import com.ww.mall.promotion.enums.GroupMemberStatus;
import com.ww.mall.promotion.enums.GroupStatus;
import com.ww.mall.promotion.event.GroupEvent;
import com.ww.mall.promotion.key.GroupRedisKeyBuilder;
import com.ww.mall.promotion.mq.GroupMqConstant;
import com.ww.mall.promotion.mq.GroupRefundMessage;
import com.ww.mall.promotion.mq.GroupSuccessMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 拼团事件批量处理器
 * 使用Disruptor批量处理拼团相关事件，提高性能和可靠性
 *
 * @author ww
 * @create 2025-12-08
 */
@Slf4j
@Component
public class GroupEventProcessor implements BatchEventProcessor<GroupEvent> {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private GroupRedisKeyBuilder groupRedisKeyBuilder;

    @Resource
    private RabbitMqPublisher rabbitMqPublisher;

    @Override
    public ProcessResult processBatch(EventBatch<GroupEvent> batch) {
        List<Event<GroupEvent>> events = batch.getEvents();
        if (CollectionUtil.isEmpty(events)) {
            return ProcessResult.success("批次为空，跳过处理");
        }

        log.debug("开始批量处理拼团事件，数量: {}", events.size());

        int successCount = 0;
        int failCount = 0;

        for (Event<GroupEvent> event : events) {
            try {
                GroupEvent groupEvent = event.getPayload();
                if (groupEvent == null) {
                    log.warn("事件负载为空，跳过: eventId={}", event.getEventId());
                    failCount++;
                    continue;
                }

                GroupEvent.EventType eventType = groupEvent.getEventType();
                if (eventType == null) {
                    log.warn("事件类型为空，跳过: eventId={}", event.getEventId());
                    failCount++;
                    continue;
                }

                switch (eventType) {
                    case GROUP_SUCCESS:
                        handleGroupSuccess(groupEvent);
                        successCount++;
                        break;
                    case GROUP_FAILED:
                        handleGroupFailed(groupEvent);
                        successCount++;
                        break;
                    case SAVE_INSTANCE:
                        handleSaveInstance(groupEvent);
                        successCount++;
                        break;
                    case SAVE_MEMBER:
                        handleSaveMember(groupEvent);
                        successCount++;
                        break;
                    default:
                        log.warn("未知的事件类型: eventType={}, eventId={}", eventType, event.getEventId());
                        failCount++;
                        break;
                }
            } catch (Exception e) {
                log.error("处理拼团事件异常: eventId={}, groupId={}",
                        event.getEventId(),
                        event.getPayload() != null ? event.getPayload().getGroupId() : "unknown",
                        e);
                failCount++;
            }
        }

        log.info("批量处理拼团事件完成，成功: {}, 失败: {}", successCount, failCount);
        return ProcessResult.success(String.format("处理完成，成功: %d, 失败: %d", successCount, failCount));
    }

    /**
     * 处理拼团成功事件
     */
    private void handleGroupSuccess(GroupEvent event) {
        String groupId = event.getGroupId();
        log.info("处理拼团成功事件: groupId={}", groupId);

        try {
            // 1. 更新Redis状态（如果还未更新）
            String metaKey = groupRedisKeyBuilder.buildGroupMetaKey(groupId);
            String currentStatus = (String) stringRedisTemplate.opsForHash().get(metaKey, "status");
            if (!GroupStatus.SUCCESS.getCode().equals(currentStatus)) {
                stringRedisTemplate.opsForHash().put(metaKey, "status", GroupStatus.SUCCESS.getCode());
                stringRedisTemplate.opsForHash().put(metaKey, "completeTime", String.valueOf(System.currentTimeMillis()));
            }

            // 2. 更新MongoDB状态
            Query query = GroupInstance.buildIdQuery(groupId);
            mongoTemplate.updateFirst(query,
                    GroupInstance.buildStatusUpdate(GroupStatus.SUCCESS.getCode()),
                    GroupInstance.class);

            // 3. 发送拼团成功消息到MQ
            sendGroupSuccessMessage(groupId);
        } catch (Exception e) {
            log.error("处理拼团成功事件异常: groupId={}", groupId, e);
            throw e; // 重新抛出异常，让Disruptor记录失败
        }
    }

    /**
     * 处理拼团失败事件
     */
    private void handleGroupFailed(GroupEvent event) {
        String groupId = event.getGroupId();
        log.info("处理拼团失败事件: groupId={}", groupId);

        try {
            // 1. 更新Redis状态
            String metaKey = groupRedisKeyBuilder.buildGroupMetaKey(groupId);
            stringRedisTemplate.opsForHash().put(metaKey, "status", GroupStatus.FAILED.getCode());
            stringRedisTemplate.opsForHash().put(metaKey, "failedTime", String.valueOf(System.currentTimeMillis()));

            // 2. 更新MongoDB状态
            Query query = GroupInstance.buildIdQuery(groupId);
            mongoTemplate.updateFirst(query,
                    GroupInstance.buildStatusUpdate(GroupStatus.FAILED.getCode()),
                    GroupInstance.class);

            // 3. 获取拼团成员信息，发送退款消息
            Query memberQuery = GroupMember.buildGroupInstanceIdQuery(groupId);
            List<GroupMember> members = mongoTemplate.find(memberQuery, GroupMember.class);
            if (CollectionUtil.isNotEmpty(members)) {
                String reason = event.getErrorMessage() != null ? event.getErrorMessage() : "拼团失败";
                sendGroupRefundMessage(groupId, members, reason);
            }
        } catch (Exception e) {
            log.error("处理拼团失败事件异常: groupId={}", groupId, e);
            throw e;
        }
    }

    /**
     * 处理保存拼团实例事件
     */
    private void handleSaveInstance(GroupEvent event) {
        String groupId = event.getGroupId();
        log.debug("处理保存拼团实例事件: groupId={}", groupId);

        try {
            // 从Redis获取拼团信息
            String metaKey = groupRedisKeyBuilder.buildGroupMetaKey(groupId);
            Map<Object, Object> meta = stringRedisTemplate.opsForHash().entries(metaKey);
            if (meta.isEmpty()) {
                log.warn("拼团实例不存在于Redis，跳过保存: groupId={}", groupId);
                return;
            }

            // 检查MongoDB中是否已存在
            GroupInstance existing = mongoTemplate.findOne(
                    GroupInstance.buildIdQuery(groupId), GroupInstance.class);
            if (existing != null) {
                log.debug("拼团实例已存在于MongoDB，跳过保存: groupId={}", groupId);
                return;
            }

            // 构建GroupInstance对象
            GroupInstance instance = new GroupInstance();
            instance.setId(groupId);
            instance.setActivityId(String.valueOf(meta.get("activityId")));
            instance.setLeaderUserId(Long.valueOf(String.valueOf(meta.get("leaderUserId"))));
            instance.setStatus(String.valueOf(meta.get("status")));
            instance.setRequiredSize(Integer.valueOf(String.valueOf(meta.get("requiredSize"))));
            instance.setCurrentSize(Integer.valueOf(String.valueOf(meta.getOrDefault("currentSize", "1"))));

            String slotsKey = groupRedisKeyBuilder.buildGroupSlotsKey(groupId);
            String slotsStr = stringRedisTemplate.opsForValue().get(slotsKey);
            instance.setRemainingSlots(slotsStr != null ? Integer.parseInt(slotsStr) : 0);

            String expiresAtStr = String.valueOf(meta.get("expiresAt"));
            if (expiresAtStr != null && !"null".equals(expiresAtStr)) {
                instance.setExpireTime(new Date(Long.parseLong(expiresAtStr)));
            }

            // 从扩展信息中获取其他字段
            if (event.getExtInfo() != null) {
                Object groupPrice = event.getExtInfo().get("groupPrice");
                Object spuId = event.getExtInfo().get("spuId");
                Object skuId = event.getExtInfo().get("skuId");
                if (groupPrice != null) instance.setGroupPrice((java.math.BigDecimal) groupPrice);
                if (spuId != null) instance.setSpuId(Long.valueOf(spuId.toString()));
                if (skuId != null) instance.setSkuId(Long.valueOf(skuId.toString()));
            }

            mongoTemplate.save(instance);

            // 保存团长成员信息
            if (event.getUserId() != null && event.getOrderId() != null) {
                GroupMember leader = new GroupMember();
                leader.setGroupInstanceId(groupId);
                leader.setActivityId(instance.getActivityId());
                leader.setUserId(event.getUserId());
                leader.setOrderId(event.getOrderId());
                leader.setIsLeader(1);
                leader.setJoinTime(new Date());
                if (event.getExtInfo() != null) {
                    Object groupPrice = event.getExtInfo().get("groupPrice");
                    Object spuId = event.getExtInfo().get("spuId");
                    Object skuId = event.getExtInfo().get("skuId");
                    if (groupPrice != null) leader.setGroupPrice((java.math.BigDecimal) groupPrice);
                    if (spuId != null) leader.setSpuId(Long.valueOf(spuId.toString()));
                    if (skuId != null) leader.setSkuId(Long.valueOf(skuId.toString()));
                }
                leader.setStatus(GroupMemberStatus.NORMAL.getCode());
                mongoTemplate.save(leader);
            }
        } catch (Exception e) {
            log.error("保存拼团实例到MongoDB异常: groupId={}", groupId, e);
            // 不抛出异常，允许后续重试
        }
    }

    /**
     * 处理保存拼团成员事件
     */
    private void handleSaveMember(GroupEvent event) {
        String groupId = event.getGroupId();
        log.debug("处理保存拼团成员事件: groupId={}, userId={}", groupId, event.getUserId());

        try {
            GroupInstance instance = mongoTemplate.findOne(
                    GroupInstance.buildIdQuery(groupId), GroupInstance.class);
            if (instance == null) {
                log.warn("拼团实例不存在，跳过保存成员: groupId={}", groupId);
                return;
            }

            // 检查成员是否已存在
            if (event.getUserId() != null) {
                Query memberQuery = GroupMember.buildGroupInstanceIdAndUserIdQuery(groupId, event.getUserId());
                GroupMember existing = mongoTemplate.findOne(memberQuery, GroupMember.class);
                if (existing != null) {
                    log.debug("成员已存在，跳过保存: groupId={}, userId={}", groupId, event.getUserId());
                    return;
                }
            }

            GroupMember member = new GroupMember();
            member.setGroupInstanceId(groupId);
            member.setActivityId(instance.getActivityId());
            member.setUserId(event.getUserId());
            member.setOrderId(event.getOrderId());
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
        } catch (Exception e) {
            log.error("保存拼团成员到MongoDB异常: groupId={}, userId={}", groupId, event.getUserId(), e);
            // 不抛出异常，允许后续重试
        }
    }

    /**
     * 发送拼团成功消息
     */
    private void sendGroupSuccessMessage(String groupId) {
        try {
            GroupInstance instance = mongoTemplate.findOne(
                    GroupInstance.buildIdQuery(groupId), GroupInstance.class);
            if (instance == null) {
                log.warn("拼团实例不存在，无法发送成功消息: groupId={}", groupId);
                return;
            }

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
            // 不抛出异常，避免影响主流程
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
                log.warn("拼团实例不存在，无法发送退款消息: groupId={}", groupId);
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
            // 不抛出异常，避免影响主流程
        }
    }

    @Override
    public int getBatchSize() {
        return 100; // 每批处理100条
    }

    @Override
    public long getBatchTimeout() {
        return 200L; // 200ms超时
    }
}

