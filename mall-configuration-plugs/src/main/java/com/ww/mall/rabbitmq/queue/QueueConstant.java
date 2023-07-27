package com.ww.mall.rabbitmq.queue;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/21 22:56
 **/
public class QueueConstant {

    private QueueConstant() {}

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

}
