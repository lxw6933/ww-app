package com.ww.mall.promotion.processor;

import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ww.app.disruptor.model.Event;
import com.ww.app.disruptor.model.EventBatch;
import com.ww.app.disruptor.model.ProcessResult;
import com.ww.app.disruptor.processor.BatchEventProcessor;
import com.ww.app.mongodb.handler.MongoBulkDataHandler;
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
import com.ww.mall.promotion.service.group.support.GroupFlowLogSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 拼团事件批量处理器。
 * <p>
 * 处理原则：
 * 1. 先落实例/成员，再处理成团/失败状态，避免最后一人尚未落库时就发送通知。
 * 2. 关键节点统一记录 traceId 维度链路日志，补齐异常排查闭环。
 * 3. 每批次同步 Redis 快照到 Mongo，减少读降级时的数据偏差。
 *
 * @author ww
 * @create 2025-12-08
 * @description: 拼团事件批量处理器
 */
@Slf4j
@Component
public class GroupEventProcessor implements BatchEventProcessor<GroupEvent> {

    private static final String PROCESSOR_SOURCE = "GROUP_PROCESSOR";

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private GroupRedisKeyBuilder groupRedisKeyBuilder;

    @Resource
    private RabbitMqPublisher rabbitMqPublisher;

    @Resource
    private MongoBulkDataHandler<GroupInstance> groupInstanceBulkHandler;

    @Resource
    private MongoBulkDataHandler<GroupMember> groupMemberBulkHandler;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private GroupFlowLogSupport groupFlowLogSupport;

    @Override
    public ProcessResult processBatch(EventBatch<GroupEvent> batch) {
        List<Event<GroupEvent>> events = batch.getEvents();
        if (CollectionUtil.isEmpty(events)) {
            return ProcessResult.success("批次为空，跳过处理");
        }

        int successCount = 0;
        int failCount = 0;
        List<GroupInstance> instancesToSave = new ArrayList<>();
        List<GroupMember> membersToSave = new ArrayList<>();
        List<GroupEvent> statusEvents = new ArrayList<>();
        Set<String> groupsToSync = new LinkedHashSet<>();

        for (Event<GroupEvent> event : events) {
            GroupEvent groupEvent = event.getPayload();
            if (groupEvent == null || groupEvent.getEventType() == null) {
                failCount++;
                log.warn("拼团事件为空或缺少类型: eventId={}", event.getEventId());
                groupFlowLogSupport.record(null, null, null, null, null,
                        "UNKNOWN_EVENT", PROCESSOR_SOURCE, "FAILED", null,
                        "事件为空或缺少类型", event);
                continue;
            }

            try {
                switch (groupEvent.getEventType()) {
                    case SAVE_INSTANCE:
                        Optional<SaveInstanceResult> saveInstanceResult = handleSaveInstance(groupEvent);
                        saveInstanceResult.ifPresent(result -> {
                            instancesToSave.add(result.getInstance());
                            if (result.getLeader() != null) {
                                membersToSave.add(result.getLeader());
                            }
                            groupsToSync.add(groupEvent.getGroupId());
                        });
                        successCount++;
                        break;
                    case SAVE_MEMBER:
                        Optional<GroupMember> saveMemberResult = handleSaveMember(groupEvent);
                        saveMemberResult.ifPresent(member -> {
                            membersToSave.add(member);
                            groupsToSync.add(groupEvent.getGroupId());
                        });
                        successCount++;
                        break;
                    case GROUP_SUCCESS:
                    case GROUP_FAILED:
                        statusEvents.add(groupEvent);
                        groupsToSync.add(groupEvent.getGroupId());
                        successCount++;
                        break;
                    default:
                        failCount++;
                        groupFlowLogSupport.recordEvent(groupEvent, PROCESSOR_SOURCE, "FAILED", null,
                                "未知拼团事件类型");
                        break;
                }
            } catch (Exception e) {
                failCount++;
                log.error("处理拼团事件异常: eventType={}, groupId={}, traceId={}",
                        groupEvent.getEventType(), groupEvent.getGroupId(), groupEvent.getTraceId(), e);
                groupFlowLogSupport.recordEvent(groupEvent, PROCESSOR_SOURCE, "FAILED", null, e.getMessage());
            }
        }

        bulkSaveInstances(instancesToSave);
        bulkSaveMembers(membersToSave);
        syncGroupSnapshotsToMongo(groupsToSync);

        for (GroupEvent statusEvent : statusEvents) {
            try {
                if (GroupEvent.EventType.GROUP_SUCCESS == statusEvent.getEventType()) {
                    handleGroupSuccess(statusEvent);
                } else if (GroupEvent.EventType.GROUP_FAILED == statusEvent.getEventType()) {
                    handleGroupFailed(statusEvent);
                }
            } catch (Exception e) {
                failCount++;
                log.error("处理拼团状态事件异常: eventType={}, groupId={}, traceId={}",
                        statusEvent.getEventType(), statusEvent.getGroupId(), statusEvent.getTraceId(), e);
                groupFlowLogSupport.recordEvent(statusEvent, PROCESSOR_SOURCE, "FAILED", null, e.getMessage());
            }
        }

        log.info("拼团事件批量处理完成: total={}, success={}, failed={}", events.size(), successCount, failCount);
        return ProcessResult.success(String.format("处理完成: success=%d, failed=%d", successCount, failCount));
    }

    /**
     * 批量保存拼团实例。
     *
     * @param instances 拼团实例列表
     */
    private void bulkSaveInstances(List<GroupInstance> instances) {
        if (CollectionUtil.isEmpty(instances)) {
            return;
        }
        try {
            groupInstanceBulkHandler.bulkSave(instances);
        } catch (Exception e) {
            log.error("批量保存拼团实例失败: size={}", instances.size(), e);
            groupFlowLogSupport.record(null, null, null, null, null,
                    "SAVE_INSTANCE_BULK", PROCESSOR_SOURCE, "FAILED", null, e.getMessage(), instances);
        }
    }

    /**
     * 批量保存拼团成员。
     *
     * @param members 拼团成员列表
     */
    private void bulkSaveMembers(List<GroupMember> members) {
        if (CollectionUtil.isEmpty(members)) {
            return;
        }
        try {
            groupMemberBulkHandler.bulkSave(members);
        } catch (Exception e) {
            log.error("批量保存拼团成员失败: size={}", members.size(), e);
            groupFlowLogSupport.record(null, null, null, null, null,
                    "SAVE_MEMBER_BULK", PROCESSOR_SOURCE, "FAILED", null, e.getMessage(), members);
        }
    }

    /**
     * 同步本批次涉及的拼团快照到 Mongo。
     *
     * @param groupIds 拼团ID集合
     */
    private void syncGroupSnapshotsToMongo(Set<String> groupIds) {
        if (CollectionUtil.isEmpty(groupIds)) {
            return;
        }
        for (String groupId : groupIds) {
            try {
                syncGroupSnapshotToMongo(groupId);
            } catch (Exception e) {
                log.error("同步拼团快照失败: groupId={}", groupId, e);
                groupFlowLogSupport.record(null, groupId, null, null, null,
                        "SYNC_GROUP_SNAPSHOT", PROCESSOR_SOURCE, "FAILED", null, e.getMessage(), null);
            }
        }
    }

    /**
     * 处理拼团成功事件。
     *
     * @param event 拼团成功事件
     */
    private void handleGroupSuccess(GroupEvent event) {
        String groupId = event.getGroupId();
        long completeMillis = getLongExt(event, "completeTime", System.currentTimeMillis());
        String metaKey = groupRedisKeyBuilder.buildGroupMetaKey(groupId);
        stringRedisTemplate.opsForHash().put(metaKey, "status", GroupStatus.SUCCESS.getCode());
        stringRedisTemplate.opsForHash().put(metaKey, "completeTime", String.valueOf(completeMillis));

        Update update = new Update()
                .set("status", GroupStatus.SUCCESS.getCode())
                .set("completeTime", new Date(completeMillis))
                .set("updateTime", new Date());
        mongoTemplate.updateFirst(GroupInstance.buildIdQuery(groupId), update, GroupInstance.class);

        sendGroupSuccessMessage(event);
        groupFlowLogSupport.recordEvent(event, PROCESSOR_SOURCE, "SUCCESS", null, null);
    }

    /**
     * 处理拼团失败事件。
     *
     * @param event 拼团失败事件
     */
    private void handleGroupFailed(GroupEvent event) {
        String groupId = event.getGroupId();
        long failedMillis = getLongExt(event, "failedTime", System.currentTimeMillis());
        String metaKey = groupRedisKeyBuilder.buildGroupMetaKey(groupId);
        stringRedisTemplate.opsForHash().put(metaKey, "status", GroupStatus.FAILED.getCode());
        stringRedisTemplate.opsForHash().put(metaKey, "failedTime", String.valueOf(failedMillis));

        Update update = new Update()
                .set("status", GroupStatus.FAILED.getCode())
                .set("failedTime", new Date(failedMillis))
                .set("updateTime", new Date());
        mongoTemplate.updateFirst(GroupInstance.buildIdQuery(groupId), update, GroupInstance.class);

        List<GroupMember> members = mongoTemplate.find(GroupMember.buildGroupInstanceIdQuery(groupId), GroupMember.class);
        if (CollectionUtil.isEmpty(members)) {
            groupFlowLogSupport.recordEvent(event, PROCESSOR_SOURCE, "FAILED", null,
                    "拼团失败但未查询到可退款成员");
            return;
        }

        String reason = event.getErrorMessage() != null && !event.getErrorMessage().trim().isEmpty()
                ? event.getErrorMessage()
                : "拼团失败";
        sendGroupRefundMessage(event.getTraceId(), groupId, members, reason);
        groupFlowLogSupport.recordEvent(event, PROCESSOR_SOURCE, "SUCCESS", null, null);
    }

    /**
     * 处理保存拼团实例事件。
     *
     * @param event 保存实例事件
     * @return 拼团实例与团长成员
     */
    private Optional<SaveInstanceResult> handleSaveInstance(GroupEvent event) {
        String groupId = event.getGroupId();
        Map<Object, Object> meta = stringRedisTemplate.opsForHash()
                .entries(groupRedisKeyBuilder.buildGroupMetaKey(groupId));
        if (meta.isEmpty()) {
            groupFlowLogSupport.recordEvent(event, PROCESSOR_SOURCE, "FAILED", null,
                    "Redis 中不存在拼团实例元数据");
            return Optional.empty();
        }

        GroupInstance existing = mongoTemplate.findOne(GroupInstance.buildIdQuery(groupId), GroupInstance.class);
        if (existing != null) {
            groupFlowLogSupport.recordEvent(event, PROCESSOR_SOURCE, "SKIPPED", null,
                    "Mongo 中已存在拼团实例");
            return Optional.empty();
        }

        OrderProductSnapshot orderProductSnapshot = resolveOrderProductSnapshot(event);
        long createdAt = getLongExt(event, "createdAt", System.currentTimeMillis());

        GroupInstance instance = new GroupInstance();
        instance.setId(groupId);
        instance.setActivityId(event.getActivityId());
        instance.setLeaderUserId(event.getUserId());
        instance.setStatus(getStringExt(event, "status", GroupStatus.OPEN.getCode()));
        instance.setRequiredSize(getIntegerExt(event, "requiredSize", 1));
        instance.setCurrentSize(getIntegerExt(event, "currentSize", 1));
        instance.setRemainingSlots(getIntegerExt(event, "remainingSlots", instance.getRequiredSize() - 1));
        instance.setExpireTime(readDate(meta.get("expiresAt")));
        instance.setGroupPrice(readBigDecimal(event.getExtInfo() != null ? event.getExtInfo().get("groupPrice") : null));
        instance.setSpuId(orderProductSnapshot.getSpuId());
        instance.setSkuId(orderProductSnapshot.getSkuId());
        instance.setCreateTime(new Date(createdAt));
        instance.setUpdateTime(new Date(createdAt));

        GroupMember leader = null;
        if (event.getUserId() != null && event.getOrderId() != null) {
            leader = new GroupMember();
            leader.setGroupInstanceId(groupId);
            leader.setActivityId(instance.getActivityId());
            leader.setUserId(event.getUserId());
            leader.setOrderId(event.getOrderId());
            leader.setIsLeader(1);
            leader.setJoinTime(new Date(createdAt));
            leader.setGroupPrice(instance.getGroupPrice());
            leader.setSpuId(orderProductSnapshot.getSpuId());
            leader.setSkuId(orderProductSnapshot.getSkuId());
            leader.setStatus(GroupMemberStatus.NORMAL.getCode());
            leader.setCreateTime(new Date(createdAt));
            leader.setUpdateTime(new Date(createdAt));
        }

        groupFlowLogSupport.recordEvent(event, PROCESSOR_SOURCE, "SUCCESS", null, null);
        return Optional.of(new SaveInstanceResult(instance, leader));
    }

    /**
     * 处理保存拼团成员事件。
     *
     * @param event 保存成员事件
     * @return 拼团成员
     */
    private Optional<GroupMember> handleSaveMember(GroupEvent event) {
        String groupId = event.getGroupId();
        if (event.getUserId() != null) {
            GroupMember existing = mongoTemplate.findOne(
                    GroupMember.buildGroupInstanceIdAndUserIdQuery(groupId, event.getUserId()),
                    GroupMember.class);
            if (existing != null) {
                groupFlowLogSupport.recordEvent(event, PROCESSOR_SOURCE, "SKIPPED", null,
                        "成员已存在，跳过重复保存");
                return Optional.empty();
            }
        }

        OrderProductSnapshot orderProductSnapshot = resolveOrderProductSnapshot(event);
        long joinTime = getLongExt(event, "joinTime", System.currentTimeMillis());

        GroupMember member = new GroupMember();
        member.setGroupInstanceId(groupId);
        member.setActivityId(event.getActivityId());
        member.setUserId(event.getUserId());
        member.setOrderId(event.getOrderId());
        member.setIsLeader(0);
        member.setJoinTime(new Date(joinTime));
        member.setGroupPrice(readBigDecimal(event.getExtInfo() != null ? event.getExtInfo().get("groupPrice") : null));
        member.setSpuId(orderProductSnapshot.getSpuId());
        member.setSkuId(orderProductSnapshot.getSkuId());
        member.setStatus(GroupMemberStatus.NORMAL.getCode());
        member.setCreateTime(new Date(joinTime));
        member.setUpdateTime(new Date(joinTime));

        groupFlowLogSupport.recordEvent(event, PROCESSOR_SOURCE, "SUCCESS", null, null);
        return Optional.of(member);
    }

    /**
     * 发送拼团成功通知消息。
     * <p>
     * 若最后一个参团成员尚未在 Mongo 中可见，则使用成功事件上下文补齐最后一单。
     *
     * @param event 拼团成功事件
     */
    private void sendGroupSuccessMessage(GroupEvent event) {
        String groupId = event.getGroupId();
        try {
            GroupInstance instance = mongoTemplate.findOne(GroupInstance.buildIdQuery(groupId), GroupInstance.class);
            if (instance == null) {
                groupFlowLogSupport.recordEvent(event, PROCESSOR_SOURCE, "FAILED", null,
                        "Mongo 中不存在拼团实例，无法发送成功通知");
                return;
            }

            List<GroupMember> members = mongoTemplate.find(GroupMember.buildGroupInstanceIdQuery(groupId), GroupMember.class);
            List<GroupSuccessMessage.MemberOrder> memberOrders = buildMemberOrders(event, members);
            if (CollectionUtil.isEmpty(memberOrders)) {
                groupFlowLogSupport.recordEvent(event, PROCESSOR_SOURCE, "FAILED", null,
                        "拼团成功但未查询到成员订单");
                return;
            }

            GroupSuccessMessage message = new GroupSuccessMessage();
            message.setTraceId(event.getTraceId());
            message.setGroupId(groupId);
            message.setActivityId(instance.getActivityId());
            message.setCompleteTime(instance.getCompleteTime());
            message.setMemberOrders(memberOrders);
            rabbitMqPublisher.sendMsg(GroupMqConstant.GROUP_EXCHANGE, GroupMqConstant.GROUP_SUCCESS_KEY, message);

            groupFlowLogSupport.record(event.getTraceId(), groupId, instance.getActivityId(), event.getUserId(),
                    event.getOrderId(), "GROUP_SUCCESS_MQ", PROCESSOR_SOURCE, "SUCCESS", null, null, message);
        } catch (Exception e) {
            log.error("发送拼团成功通知失败: groupId={}, traceId={}", groupId, event.getTraceId(), e);
            groupFlowLogSupport.recordEvent(event, PROCESSOR_SOURCE, "FAILED", null, e.getMessage());
        }
    }

    /**
     * 组装成功通知成员列表。
     *
     * @param event 拼团成功事件
     * @param members Mongo 中成员列表
     * @return 成员订单列表
     */
    private List<GroupSuccessMessage.MemberOrder> buildMemberOrders(GroupEvent event, List<GroupMember> members) {
        List<GroupSuccessMessage.MemberOrder> memberOrders = CollectionUtil.isEmpty(members)
                ? new ArrayList<>()
                : members.stream().map(member -> {
                    GroupSuccessMessage.MemberOrder order = new GroupSuccessMessage.MemberOrder();
                    order.setUserId(member.getUserId());
                    order.setOrderId(member.getOrderId());
                    order.setIsLeader(member.getIsLeader() == 1);
                    return order;
                }).collect(Collectors.toList());

        if (event.getUserId() != null && event.getOrderId() != null) {
            boolean exists = memberOrders.stream().anyMatch(item ->
                    event.getUserId().equals(item.getUserId()) && event.getOrderId().equals(item.getOrderId()));
            if (!exists) {
                GroupSuccessMessage.MemberOrder order = new GroupSuccessMessage.MemberOrder();
                order.setUserId(event.getUserId());
                order.setOrderId(event.getOrderId());
                order.setIsLeader(Boolean.FALSE);
                memberOrders.add(order);
            }
        }
        return memberOrders;
    }

    /**
     * 发送拼团退款消息。
     *
     * @param traceId 链路追踪ID
     * @param groupId 拼团ID
     * @param members 退款成员
     * @param reason 退款原因
     */
    private void sendGroupRefundMessage(String traceId, String groupId, List<GroupMember> members, String reason) {
        try {
            GroupInstance instance = mongoTemplate.findOne(GroupInstance.buildIdQuery(groupId), GroupInstance.class);
            if (instance == null) {
                groupFlowLogSupport.record(traceId, groupId, null, null, null,
                        "GROUP_REFUND_MQ", PROCESSOR_SOURCE, "FAILED", null,
                        "Mongo 中不存在拼团实例，无法发送退款消息", members);
                return;
            }

            List<GroupRefundMessage.RefundOrder> refundOrders = members.stream().map(member -> {
                GroupRefundMessage.RefundOrder order = new GroupRefundMessage.RefundOrder();
                order.setUserId(member.getUserId());
                order.setOrderId(member.getOrderId());
                order.setRefundAmount(member.getGroupPrice());
                order.setIsLeader(member.getIsLeader() == 1);
                return order;
            }).collect(Collectors.toList());

            GroupRefundMessage message = new GroupRefundMessage();
            message.setTraceId(traceId);
            message.setGroupId(groupId);
            message.setActivityId(instance.getActivityId());
            message.setReason(reason);
            message.setRefundOrders(refundOrders);
            rabbitMqPublisher.sendMsg(GroupMqConstant.GROUP_EXCHANGE, GroupMqConstant.GROUP_REFUND_KEY, message);

            groupFlowLogSupport.record(traceId, groupId, instance.getActivityId(), null, null,
                    "GROUP_REFUND_MQ", PROCESSOR_SOURCE, "SUCCESS", null, null, message);
        } catch (Exception e) {
            log.error("发送拼团退款消息失败: groupId={}, traceId={}", groupId, traceId, e);
            groupFlowLogSupport.record(traceId, groupId, null, null, null,
                    "GROUP_REFUND_MQ", PROCESSOR_SOURCE, "FAILED", null, e.getMessage(), members);
        }
    }

    @Override
    public int getBatchSize() {
        return 100;
    }

    @Override
    public long getBatchTimeout() {
        return 200L;
    }

    /**
     * 将 Redis 拼团快照同步到 Mongo。
     *
     * @param groupId 拼团ID
     */
    private void syncGroupSnapshotToMongo(String groupId) {
        if (groupId == null || groupId.trim().isEmpty()) {
            return;
        }

        Map<Object, Object> meta = stringRedisTemplate.opsForHash()
                .entries(groupRedisKeyBuilder.buildGroupMetaKey(groupId));
        if (meta.isEmpty()) {
            return;
        }

        GroupInstance instance = mongoTemplate.findOne(GroupInstance.buildIdQuery(groupId), GroupInstance.class);
        if (instance == null) {
            return;
        }

        String slotsValue = stringRedisTemplate.opsForValue().get(groupRedisKeyBuilder.buildGroupSlotsKey(groupId));
        Update update = new Update().set("updateTime", new Date());
        putIfNotNull(update, "activityId", meta.get("activityId"));
        putIfNotNull(update, "leaderUserId", toLong(meta.get("leaderUserId")));
        putIfNotNull(update, "status", meta.get("status"));
        putIfNotNull(update, "currentSize", toInteger(meta.get("currentSize")));
        putIfNotNull(update, "remainingSlots", toInteger(slotsValue));
        putIfNotNull(update, "expireTime", readDate(meta.get("expiresAt")));
        putIfNotNull(update, "completeTime", readDate(meta.get("completeTime")));
        putIfNotNull(update, "failedTime", readDate(meta.get("failedTime")));
        mongoTemplate.updateFirst(GroupInstance.buildIdQuery(groupId), update, GroupInstance.class);
    }

    /**
     * 解析订单中的商品维度信息。
     *
     * @param event 拼团事件
     * @return 商品快照
     */
    private OrderProductSnapshot resolveOrderProductSnapshot(GroupEvent event) {
        OrderProductSnapshot snapshot = new OrderProductSnapshot();
        if (event.getExtInfo() != null) {
            snapshot.setSpuId(toLong(event.getExtInfo().get("spuId")));
        }
        if (event.getOrderInfo() == null || event.getOrderInfo().trim().isEmpty()) {
            return snapshot;
        }

        try {
            JsonNode root = objectMapper.readTree(event.getOrderInfo());
            snapshot.setSpuId(firstNonNull(snapshot.getSpuId(), readLong(root, "spuId"), readLong(root, "spuCode")));
            snapshot.setSkuId(firstNonNull(snapshot.getSkuId(), readLong(root, "skuId"), readLong(root, "skuCode")));
        } catch (Exception e) {
            log.warn("解析订单商品信息失败: groupId={}, orderId={}, traceId={}",
                    event.getGroupId(), event.getOrderId(), event.getTraceId(), e);
            groupFlowLogSupport.recordEvent(event, PROCESSOR_SOURCE, "FAILED", null,
                    "解析订单商品信息失败: " + e.getMessage());
        }
        return snapshot;
    }

    /**
     * 读取 Long 字段。
     *
     * @param node JSON 节点
     * @param fieldName 字段名
     * @return Long 值
     */
    private Long readLong(JsonNode node, String fieldName) {
        if (node == null || fieldName == null || fieldName.trim().isEmpty()) {
            return null;
        }
        JsonNode fieldNode = node.get(fieldName);
        return toLong(fieldNode == null || fieldNode.isNull() ? null : fieldNode.asText());
    }

    /**
     * 获取扩展字段中的 Integer。
     *
     * @param event 拼团事件
     * @param key 扩展字段名
     * @param defaultVal 默认值
     * @return Integer 值
     */
    private Integer getIntegerExt(GroupEvent event, String key, Integer defaultVal) {
        if (event.getExtInfo() == null) {
            return defaultVal;
        }
        Integer value = toInteger(event.getExtInfo().get(key));
        return value != null ? value : defaultVal;
    }

    /**
     * 获取扩展字段中的 Long。
     *
     * @param event 拼团事件
     * @param key 扩展字段名
     * @param defaultVal 默认值
     * @return Long 值
     */
    private Long getLongExt(GroupEvent event, String key, Long defaultVal) {
        if (event.getExtInfo() == null) {
            return defaultVal;
        }
        Long value = toLong(event.getExtInfo().get(key));
        return value != null ? value : defaultVal;
    }

    /**
     * 获取扩展字段中的 String。
     *
     * @param event 拼团事件
     * @param key 扩展字段名
     * @param defaultVal 默认值
     * @return String 值
     */
    private String getStringExt(GroupEvent event, String key, String defaultVal) {
        if (event.getExtInfo() == null) {
            return defaultVal;
        }
        Object value = event.getExtInfo().get(key);
        return value != null ? String.valueOf(value) : defaultVal;
    }

    /**
     * 安全转换为 BigDecimal。
     *
     * @param value 待转换值
     * @return BigDecimal
     */
    private BigDecimal readBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将对象转换为日期。
     *
     * @param value 待转换值
     * @return 日期
     */
    private Date readDate(Object value) {
        Long millis = toLong(value);
        return millis == null ? null : new Date(millis);
    }

    /**
     * 安全转换为 Long。
     *
     * @param value 待转换值
     * @return Long 值
     */
    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            String stringValue = String.valueOf(value).trim();
            if (stringValue.isEmpty() || "null".equalsIgnoreCase(stringValue)) {
                return null;
            }
            return Long.parseLong(stringValue);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 安全转换为 Integer。
     *
     * @param value 待转换值
     * @return Integer 值
     */
    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            String stringValue = String.valueOf(value).trim();
            if (stringValue.isEmpty() || "null".equalsIgnoreCase(stringValue)) {
                return null;
            }
            return Integer.parseInt(stringValue);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 若值非空则写入 Update。
     *
     * @param update Mongo 更新对象
     * @param field 字段名
     * @param value 字段值
     */
    private void putIfNotNull(Update update, String field, Object value) {
        if (value != null) {
            update.set(field, value);
        }
    }

    /**
     * 返回第一个非空值。
     *
     * @param values 候选值
     * @param <T> 泛型
     * @return 第一个非空值
     */
    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * 保存实例处理结果。
     */
    private static class SaveInstanceResult {

        /**
         * 拼团实例。
         */
        private final GroupInstance instance;

        /**
         * 团长成员。
         */
        private final GroupMember leader;

        /**
         * 构造处理结果。
         *
         * @param instance 拼团实例
         * @param leader 团长成员
         */
        private SaveInstanceResult(GroupInstance instance, GroupMember leader) {
            this.instance = instance;
            this.leader = leader;
        }

        /**
         * 获取拼团实例。
         *
         * @return 拼团实例
         */
        public GroupInstance getInstance() {
            return instance;
        }

        /**
         * 获取团长成员。
         *
         * @return 团长成员
         */
        public GroupMember getLeader() {
            return leader;
        }
    }

    /**
     * 订单商品快照。
     */
    private static class OrderProductSnapshot {

        /**
         * SPU ID。
         */
        private Long spuId;

        /**
         * SKU ID。
         */
        private Long skuId;

        /**
         * 获取 SPU ID。
         *
         * @return SPU ID
         */
        public Long getSpuId() {
            return spuId;
        }

        /**
         * 设置 SPU ID。
         *
         * @param spuId SPU ID
         */
        public void setSpuId(Long spuId) {
            this.spuId = spuId;
        }

        /**
         * 获取 SKU ID。
         *
         * @return SKU ID
         */
        public Long getSkuId() {
            return skuId;
        }

        /**
         * 设置 SKU ID。
         *
         * @param skuId SKU ID
         */
        public void setSkuId(Long skuId) {
            this.skuId = skuId;
        }
    }
}
