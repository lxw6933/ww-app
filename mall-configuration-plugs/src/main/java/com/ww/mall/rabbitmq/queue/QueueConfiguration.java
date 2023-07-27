package com.ww.mall.rabbitmq.queue;

import com.ww.mall.rabbitmq.MallRabbitmqAutoConfiguration;
import com.ww.mall.rabbitmq.exchange.ExchangeConstant;
import com.ww.mall.rabbitmq.routekey.RouteKeyConstant;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/21 22:54
 **/
@Configuration
@ConditionalOnBean(MallRabbitmqAutoConfiguration.class)
public class QueueConfiguration {

    private static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    private static final String X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";
    private static final String X_MESSAGE_TTL = "x-message-ttl";

    @Bean(name = QueueConstant.MALL_MEMBER_REGISTER_QUEUE)
    public Queue mallMemberRegisterQueue() {
        return new Queue(QueueConstant.MALL_MEMBER_REGISTER_QUEUE);
    }

    @Bean(name = QueueConstant.MALL_CANAL_QUEUE)
    public Queue mallCanalQueue() {
        return new Queue(QueueConstant.MALL_CANAL_QUEUE);
    }

    @Bean(name = QueueConstant.MALL_OMS_CLOSE_QUEUE)
    public Queue orderCloseQueue() {return new Queue(QueueConstant.MALL_OMS_CLOSE_QUEUE);}

    @Bean(name = QueueConstant.MALL_OMS_DELAY_FIFTEEN_QUEUE)
    public Queue orderDelayFiftyQueue() {
        Map<String, Object> args = new HashMap<>(256);
        args.put(X_DEAD_LETTER_EXCHANGE, ExchangeConstant.MALL_OMS_EXCHANGE);
        args.put(X_DEAD_LETTER_ROUTING_KEY, RouteKeyConstant.MALL_OMS_CLOSE_KEY);
        args.put(X_MESSAGE_TTL, 15 * 1000 * 60);
        return new Queue(QueueConstant.MALL_OMS_DELAY_FIFTEEN_QUEUE, true, false, false, args);
    }

}
