package com.ww.app.rabbitmq.queue;

import com.ww.app.rabbitmq.exchange.ExchangeConstant;
import com.ww.app.rabbitmq.routekey.RouteKeyConstant;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/21 22:54
 **/
public class QueueConfiguration {

    private static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    private static final String X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";
    private static final String X_MESSAGE_TTL = "x-message-ttl";

    private static final Map<String, Object> dlxArgMap = new HashMap<>();

    static {
        dlxArgMap.put(X_DEAD_LETTER_EXCHANGE, ExchangeConstant.APP_DLX_EXCHANGE);
        dlxArgMap.put(X_DEAD_LETTER_ROUTING_KEY, RouteKeyConstant.FAILED_ROUTING_KEY);
    }

    @Bean(name = QueueConstant.CONSUME_FAIL_QUEUE)
    public Queue mallConsumeFailQueue() {
        return new Queue(QueueConstant.CONSUME_FAIL_QUEUE);
    }

    @Bean(name = QueueConstant.CACHE_NOTICE_QUEUE)
    public Queue mallCacheNoticeQueue() {
        return new Queue(QueueConstant.CACHE_NOTICE_QUEUE);
    }

    @Bean(name = QueueConstant.CREATE_ORDER_QUEUE)
    public Queue mallCreateOrderQueue() {
        return QueueBuilder.durable(QueueConstant.CREATE_ORDER_QUEUE).withArguments(dlxArgMap).build();
    }

    @Bean(name = QueueConstant.COUPON_TEST_QUEUE)
    public Queue mallCouponTestQueue() {
        return QueueBuilder.durable(QueueConstant.COUPON_TEST_QUEUE).withArguments(dlxArgMap).build();
    }

    @Bean(name = QueueConstant.MEMBER_REGISTER_QUEUE)
    public Queue mallMemberRegisterQueue() {
        return QueueBuilder.durable(QueueConstant.MEMBER_REGISTER_QUEUE).withArguments(dlxArgMap).build();
    }

    @Bean(name = QueueConstant.CANAL_QUEUE)
    public Queue mallCanalQueue() {
        return QueueBuilder.durable(QueueConstant.CANAL_QUEUE).withArguments(dlxArgMap).build();
    }

    @Bean(name = QueueConstant.OMS_CLOSE_QUEUE)
    public Queue orderCloseQueue() {
        return QueueBuilder.durable(QueueConstant.PRODUCT_TIMER_UP_QUEUE).withArguments(dlxArgMap).build();
    }

    @Bean(name = QueueConstant.OMS_DELAY_FIFTEEN_QUEUE)
    public Queue orderDelayFiftyQueue() {
        Map<String, Object> args = new HashMap<>(256);
        args.put(X_DEAD_LETTER_EXCHANGE, ExchangeConstant.OMS_EXCHANGE);
        args.put(X_DEAD_LETTER_ROUTING_KEY, RouteKeyConstant.OMS_CLOSE_KEY);
        args.put(X_MESSAGE_TTL, 15 * 1000 * 60);
        return new Queue(QueueConstant.OMS_DELAY_FIFTEEN_QUEUE, true, false, false, args);
    }

    @Bean(name = QueueConstant.PRODUCT_TIMER_UP_QUEUE)
    public Queue productTimerUpQueue() {
        return QueueBuilder.durable(QueueConstant.PRODUCT_TIMER_UP_QUEUE).withArguments(dlxArgMap).build();
    }

    @Bean(name = QueueConstant.TEST_QUEUE)
    public Queue testQueue() {
        return QueueBuilder.durable(QueueConstant.TEST_QUEUE).withArguments(dlxArgMap).build();
    }

}
