package com.ww.mall.promotion.mq;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 拼团退款申请消息。
 * <p>
 * 该消息由拼团域在“支付后未成功入团”或“团最终失败”时发出，
 * 由下游订单域/支付域基于订单号执行真正的退款申请。
 * 设计上显式携带退款金额和失败场景，便于下游做幂等、防重和审计。
 *
 * @author ww
 * @create 2026-03-26
 * @description: 拼团退款申请消息
 */
@Data
public class GroupRefundRequestMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 拼团ID。
     * <p>
     * 支付后开团前校验失败时允许为空。
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
     * 退款金额。
     */
    private BigDecimal refundAmount;

    /**
     * 退款场景编码。
     * <p>
     * 例如：
     * 1. GROUP_CREATE_REJECTED
     * 2. GROUP_JOIN_REJECTED
     * 3. GROUP_FAILED_REFUND
     */
    private String refundScene;

    /**
     * 退款原因。
     */
    private String reason;

    /**
     * 事件发生时间。
     */
    private Date eventTime;
}
