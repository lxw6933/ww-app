package com.ww.mall.config.rabbitmq.binding;

/**
 * @description:
 * @author: ww
 * @create: 2021/6/29 下午10:30
 **/
public class RabbitMqConstant {

    /**
     * queue
     */
    public static final String DEMO_QUEUE_NAME = "demo.queue";

    public static final String INFO_QUEUE_NAME = "info.queue";

    public static final String STOCK_QUEUE_NAME = "stock.queue";

    public static final String ORDER_DELAY_QUEUE = "order-delay-queue";

    public static final String ORDER_RELEASE_QUEUE = "order-release-queue";

    /**
     * exchange
     */
    public static final String DEMO_EXCHANGE_NAME = "topic.demo.exchange";

    public static final String MAIL_EXCHANGE_NAME = "topic.email.exchange";

    public static final String ORDER_EXCHANGE_NAME = "topic.order.exchange";

    public static final String ORDER_EVENT_EXCHANGE = "order-event-exchange";

    /**
     * routing_key
     */
    public final static String DEMO_ROUTING_KEY = "demo.key";

    public final static String ORDER_ROUTING_KEY = "order.key";

    public final static String INFO_ROUTING_KEY = "info.key";

    public final static String ORDER_RELEASE_KEY = "order.release.key";

    public final static String ORDER_CREATE_KEY = "order.create.key";

}
