package com.ww.mall.promotion.mq;

/**
 * @author ww
 * @create 2025-12-08 18:10
 * @description: 拼团消息队列常量
 */
public class GroupMqConstant {

    /**
     * 拼团交换机
     */
    public static final String GROUP_EXCHANGE = "group.exchange";

    /**
     * 支付成功驱动拼团的队列。
     */
    public static final String GROUP_ORDER_PAID_QUEUE = "group.order.paid.queue";

    /**
     * 支付成功驱动拼团的路由键。
     */
    public static final String GROUP_ORDER_PAID_KEY = "group.order.paid";

    /**
     * 拼团状态变更内部驱动队列。
     */
    public static final String GROUP_STATE_CHANGED_QUEUE = "group.state.changed.queue";

    /**
     * 拼团状态变更内部驱动路由键。
     */
    public static final String GROUP_STATE_CHANGED_KEY = "group.state.changed";

    /**
     * 拼团退款补偿申请队列。
     * <p>
     * 该队列面向订单域/支付域消费，拼团域只负责投递退款申请事件，
     * 不直接在本域内执行支付退款。
     */
    public static final String GROUP_REFUND_REQUEST_QUEUE = "group.refund.request.queue";

    /**
     * 拼团退款补偿申请路由键。
     */
    public static final String GROUP_REFUND_REQUEST_KEY = "group.refund.requested";

}
