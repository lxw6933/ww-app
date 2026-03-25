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
     * 售后成功驱动拼团回退名额的队列。
     */
    public static final String GROUP_AFTER_SALE_QUEUE = "group.after.sale.queue";

    /**
     * 售后成功驱动拼团回退名额的路由键。
     */
    public static final String GROUP_AFTER_SALE_KEY = "group.after.sale.success";

    /**
     * 拼团状态变更内部驱动队列。
     */
    public static final String GROUP_STATE_CHANGED_QUEUE = "group.state.changed.queue";

    /**
     * 拼团状态变更内部驱动路由键。
     */
    public static final String GROUP_STATE_CHANGED_KEY = "group.state.changed";

}
