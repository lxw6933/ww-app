package com.ww.mall.promotion.engine;

import com.ww.app.rabbitmq.RabbitMqPublisher;
import com.ww.mall.promotion.constants.GroupBizConstants;
import com.ww.mall.promotion.engine.model.GroupCacheSnapshot;
import com.ww.mall.promotion.engine.model.GroupDomainEventType;
import com.ww.mall.promotion.engine.model.GroupProjectionEvent;
import com.ww.mall.promotion.entity.group.GroupInstance;
import com.ww.mall.promotion.entity.group.GroupMember;
import com.ww.mall.promotion.enums.GroupMemberBizStatus;
import com.ww.mall.promotion.key.GroupRedisKeyBuilder;
import com.ww.mall.promotion.mq.GroupFailedMessage;
import com.ww.mall.promotion.mq.GroupMqConstant;
import com.ww.mall.promotion.mq.GroupRefundMessage;
import com.ww.mall.promotion.mq.GroupSuccessMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 拼团领域事件投影器。
 * <p>
 * 从 Redis Stream 读取拼团事件，异步刷新 Mongo 投影，并向外部域发布成功/失败/退款 MQ。
 *
 * @author ww
 * @create 2026-03-19
 * @description: 拼团领域事件投影器
 */
@Slf4j
@Component
public class GroupDomainEventProjector {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private GroupRedisKeyBuilder groupRedisKeyBuilder;

    @Resource
    private GroupRedisStateReader groupRedisStateReader;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private RabbitMqPublisher rabbitMqPublisher;

    private final String consumerName = GroupBizConstants.GROUP_EVENT_STREAM_CONSUMER_PREFIX + UUID.randomUUID();

    /**
     * 初始化消费组。
     */
    @PostConstruct
    public void initConsumerGroup() {
        String streamKey = groupRedisKeyBuilder.buildDomainEventStreamKey();
        try {
            if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(streamKey))) {
                stringRedisTemplate.opsForStream().add(
                        MapRecord.create(streamKey, Collections.singletonMap("eventType", "INIT"))
                );
            }
            stringRedisTemplate.opsForStream().createGroup(streamKey, ReadOffset.latest(),
                    GroupBizConstants.GROUP_EVENT_STREAM_CONSUMER_GROUP);
        } catch (Exception ignore) {
        }
    }

    /**
     * 轮询消费事件。
     */
    @Scheduled(fixedDelay = GroupBizConstants.GROUP_EVENT_STREAM_FIXED_DELAY_MILLIS)
    public void consumeEvents() {
        String streamKey = groupRedisKeyBuilder.buildDomainEventStreamKey();
        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                Consumer.from(GroupBizConstants.GROUP_EVENT_STREAM_CONSUMER_GROUP, consumerName),
                StreamReadOptions.empty()
                        .count(GroupBizConstants.GROUP_EVENT_STREAM_BATCH_SIZE)
                        .block(Duration.ofMillis(GroupBizConstants.GROUP_EVENT_STREAM_BLOCK_MILLIS)),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed())
        );
        if (records == null || records.isEmpty()) {
            return;
        }
        for (MapRecord<String, Object, Object> record : records) {
            try {
                GroupProjectionEvent event = parseEvent(record);
                if (event.getEventType() != null) {
                    if (alreadyProjected(event)) {
                        acknowledge(streamKey, record.getId().getValue());
                        continue;
                    }
                    GroupCacheSnapshot snapshot = groupRedisStateReader.loadGroupSnapshot(event.getGroupId());
                    if (snapshot != null) {
                        // publish external business message first, then persist Mongo projection with lastEventId
                        publishBusinessMessage(snapshot, event);
                        persistProjection(snapshot, event);
                    } else {
                        log.warn("拼团事件投影跳过，Redis 主状态不存在: eventId={}, eventType={}, groupId={}",
                                event.getEventId(), event.getEventType(), event.getGroupId());
                    }
                }
                acknowledge(streamKey, record.getId().getValue());
            } catch (Exception e) {
                log.error("消费拼团事件失败: streamKey={}, recordId={}", streamKey, record.getId().getValue(), e);
                break;
            }
        }
    }

    /**
     * 持久化 Mongo 投影。
     *
     * @param snapshot 团快照
     * @param event 事件
     */
    private void persistProjection(GroupCacheSnapshot snapshot, GroupProjectionEvent event) {
        GroupInstance instance = snapshot.getInstance();
        instance.setLastEventId(event.getEventId());
        mongoTemplate.save(instance);
        mongoTemplate.remove(GroupMember.buildGroupInstanceIdQuery(instance.getId()), GroupMember.class);
        if (snapshot.getMembers() != null && !snapshot.getMembers().isEmpty()) {
            mongoTemplate.insert(snapshot.getMembers(), GroupMember.class);
        }
    }

    /**
     * 发布业务 MQ。
     *
     * @param snapshot 团快照
     * @param event 事件
     */
    private void publishBusinessMessage(GroupCacheSnapshot snapshot, GroupProjectionEvent event) {
        if (event.getEventType() == GroupDomainEventType.GROUP_COMPLETED) {
            GroupSuccessMessage message = new GroupSuccessMessage();
            message.setGroupId(snapshot.getInstance().getId());
            message.setActivityId(snapshot.getInstance().getActivityId());
            message.setCompleteTime(snapshot.getInstance().getCompleteTime());
            message.setMemberOrders(snapshot.getMembers().stream()
                    .filter(member -> GroupMemberBizStatus.SUCCESS.name().equals(member.getMemberStatus()))
                    .map(member -> {
                        GroupSuccessMessage.MemberOrder order = new GroupSuccessMessage.MemberOrder();
                        order.setUserId(member.getUserId());
                        order.setOrderId(member.getOrderId());
                        order.setIsLeader(member.getIsLeader() != null && member.getIsLeader() == 1);
                        return order;
                    }).collect(Collectors.toList()));
            rabbitMqPublisher.sendMsg(GroupMqConstant.GROUP_EXCHANGE, GroupMqConstant.GROUP_SUCCESS_KEY, message);
            return;
        }
        if (event.getEventType() == GroupDomainEventType.GROUP_FAILED) {
            GroupFailedMessage failedMessage = new GroupFailedMessage();
            failedMessage.setGroupId(snapshot.getInstance().getId());
            failedMessage.setActivityId(snapshot.getInstance().getActivityId());
            failedMessage.setFailedTime(snapshot.getInstance().getFailedTime());
            failedMessage.setReason(snapshot.getInstance().getFailReason());
            rabbitMqPublisher.sendMsg(GroupMqConstant.GROUP_EXCHANGE, GroupMqConstant.GROUP_FAILED_KEY, failedMessage);

            List<GroupRefundMessage.RefundOrder> refundOrders = snapshot.getMembers().stream()
                    .filter(member -> GroupMemberBizStatus.FAILED_REFUND_PENDING.name().equals(member.getMemberStatus()))
                    .map(this::buildRefundOrder)
                    .collect(Collectors.toList());
            if (!refundOrders.isEmpty()) {
                GroupRefundMessage refundMessage = new GroupRefundMessage();
                refundMessage.setGroupId(snapshot.getInstance().getId());
                refundMessage.setActivityId(snapshot.getInstance().getActivityId());
                refundMessage.setReason(snapshot.getInstance().getFailReason());
                refundMessage.setRefundOrders(refundOrders);
                rabbitMqPublisher.sendMsg(GroupMqConstant.GROUP_EXCHANGE, GroupMqConstant.GROUP_REFUND_KEY, refundMessage);
            }
        }
    }

    /**
     * 构建退款订单。
     *
     * @param member 成员
     * @return 退款订单
     */
    private GroupRefundMessage.RefundOrder buildRefundOrder(GroupMember member) {
        GroupRefundMessage.RefundOrder refundOrder = new GroupRefundMessage.RefundOrder();
        refundOrder.setUserId(member.getUserId());
        refundOrder.setOrderId(member.getOrderId());
        refundOrder.setRefundAmount(member.getPayAmount() != null ? member.getPayAmount() : BigDecimal.ZERO);
        refundOrder.setIsLeader(member.getIsLeader() != null && member.getIsLeader() == 1);
        return refundOrder;
    }

    /**
     * 解析事件。
     *
     * @param record Stream 记录
     * @return 事件对象
     */
    private GroupProjectionEvent parseEvent(MapRecord<String, Object, Object> record) {
        Map<Object, Object> value = record.getValue();
        GroupProjectionEvent event = new GroupProjectionEvent();
        event.setEventId(record.getId().getValue());
        String eventType = stringValue(value.get("eventType"));
        if ("INIT".equals(eventType)) {
            return event;
        }
        event.setEventType(GroupDomainEventType.valueOf(eventType));
        event.setGroupId(stringValue(value.get("groupId")));
        event.setActivityId(stringValue(value.get("activityId")));
        event.setUserId(parseLong(value.get("userId")));
        event.setOrderId(stringValue(value.get("orderId")));
        event.setReason(stringValue(value.get("reason")));
        event.setOccurredAt(parseLong(value.get("occurredAt")));
        return event;
    }

    /**
     * 判断事件是否已完成投影。
     * <p>
     * Redis Stream 在至少一次消费语义下，异常恢复后可能重放同一条事件。
     * 这里以 Mongo 中最近一次已落盘的 eventId 做幂等保护，避免重复发 MQ。
     *
     * @param event 投影事件
     * @return true-已完成投影
     */
    private boolean alreadyProjected(GroupProjectionEvent event) {
        if (event == null || event.getEventType() == null || !hasText(event.getGroupId())) {
            return false;
        }
        GroupInstance existing = mongoTemplate.findOne(GroupInstance.buildIdQuery(event.getGroupId()), GroupInstance.class);
        return existing != null && event.getEventId().equals(existing.getLastEventId());
    }

    /**
     * 确认消费。
     *
     * @param streamKey Stream Key
     * @param recordId 记录ID
     */
    private void acknowledge(String streamKey, String recordId) {
        stringRedisTemplate.opsForStream().acknowledge(streamKey,
                GroupBizConstants.GROUP_EVENT_STREAM_CONSUMER_GROUP, recordId);
    }

    /**
     * 读取字符串。
     *
     * @param value 原值
     * @return 字符串
     */
    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 解析长整型。
     *
     * @param value 原值
     * @return 长整型
     */
    private Long parseLong(Object value) {
        String text = stringValue(value);
        return text == null || text.trim().isEmpty() ? null : Long.parseLong(text);
    }

    /**
     * 判断文本是否有值。
     *
     * @param value 文本
     * @return true-有值
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

}
