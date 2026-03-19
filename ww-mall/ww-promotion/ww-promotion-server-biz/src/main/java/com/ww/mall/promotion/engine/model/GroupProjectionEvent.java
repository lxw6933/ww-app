package com.ww.mall.promotion.engine.model;

import lombok.Data;

/**
 * 拼团投影事件。
 *
 * @author ww
 * @create 2026-03-19
 * @description: 由 Redis Stream 读取后转换的事件对象
 */
@Data
public class GroupProjectionEvent {

    /**
     * Stream 记录ID。
     */
    private String eventId;

    /**
     * 事件类型。
     */
    private GroupDomainEventType eventType;

    /**
     * 团ID。
     */
    private String groupId;

    /**
     * 活动ID。
     */
    private String activityId;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 订单ID。
     */
    private String orderId;

    /**
     * 事件原因。
     */
    private String reason;

    /**
     * 事件发生时间毫秒值。
     */
    private Long occurredAt;
}
