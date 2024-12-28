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
    public static final String CACHE_NOTICE_QUEUE = "cache.notice.queue";

    public static final String COUPON_TEST_QUEUE = "coupon.test.queue";

    /**
     * 用户注册队列
     */
    public static final String MEMBER_REGISTER_QUEUE = "member.register.queue";

    /**
     * canal队列
     */
    public static final String CANAL_QUEUE = "canal.queue";

    public static final String OMS_CLOSE_QUEUE = "oms.close.queue";

    public static final String OMS_DELAY_FIFTEEN_QUEUE = "oms.delay_15_queue";

    public static final String PRODUCT_TIMER_UP_QUEUE = "product.timer.up.queue";

    public static final String CREATE_ORDER_QUEUE = "create.order.queue";

    public static final String TEST_QUEUE = "test.queue";

}
