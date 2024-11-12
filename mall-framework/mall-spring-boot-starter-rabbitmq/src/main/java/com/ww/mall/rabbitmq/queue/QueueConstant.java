package com.ww.mall.rabbitmq.queue;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/21 22:56
 **/
public class QueueConstant {

    private QueueConstant() {}

    /**
     * 缓存更新消息通知队列
     */
    public static final String MALL_CACHE_NOTICE_QUEUE = "mall.cache.notice.queue";

    public static final String MALL_COUPON_TEST_QUEUE = "mall.coupon.test.queue";

    /**
     * 用户注册队列
     */
    public static final String MALL_MEMBER_REGISTER_QUEUE = "mall.member.register.queue";

    /**
     * canal队列
     */
    public static final String MALL_CANAL_QUEUE = "mall.canal.queue";

    public static final String MALL_OMS_CLOSE_QUEUE = "mall.oms.close.queue";

    public static final String MALL_OMS_DELAY_FIFTEEN_QUEUE = "mall.oms.delay_15_queue";

    public static final String MALL_PRODUCT_TIMER_UP_QUEUE = "mall.product.timer.up.queue";

    public static final String MALL_CREATE_ORDER_QUEUE = "mall.create.order.queue";

    public static final String MALL_TEST_QUEUE = "mall.test.queue";

}
