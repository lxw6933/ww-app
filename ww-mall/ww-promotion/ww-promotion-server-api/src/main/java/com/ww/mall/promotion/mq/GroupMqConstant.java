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
     * 拼团成功队列
     */
    public static final String GROUP_SUCCESS_QUEUE = "group.success.queue";

    /**
     * 拼团成功路由键
     */
    public static final String GROUP_SUCCESS_KEY = "group.success";

    /**
     * 拼团失败队列
     */
    public static final String GROUP_FAILED_QUEUE = "group.failed.queue";

    /**
     * 拼团失败路由键
     */
    public static final String GROUP_FAILED_KEY = "group.failed";

    /**
     * 拼团过期队列
     */
    public static final String GROUP_EXPIRED_QUEUE = "group.expired.queue";

    /**
     * 拼团过期路由键
     */
    public static final String GROUP_EXPIRED_KEY = "group.expired";

    /**
     * 拼团退款队列
     */
    public static final String GROUP_REFUND_QUEUE = "group.refund.queue";

    /**
     * 拼团退款路由键
     */
    public static final String GROUP_REFUND_KEY = "group.refund";

}
