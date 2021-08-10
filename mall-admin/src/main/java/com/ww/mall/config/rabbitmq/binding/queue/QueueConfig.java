package com.ww.mall.config.rabbitmq.binding.queue;

import com.ww.mall.config.rabbitmq.binding.RabbitMqConstant;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @description:
 * @author: ww
 * @create: 2021/6/29 下午8:44
 **/
@Configuration
public class QueueConfig {

    @Bean(name = "demoQueue")
    public Queue demoQueue() {
        return new Queue(RabbitMqConstant.DEMO_QUEUE_NAME);
    }

    @Bean(name = "infoQueue")
    public Queue infoQueue() {
        return new Queue(RabbitMqConstant.INFO_QUEUE_NAME);
    }

    @Bean(name = "stockQueue")
    public Queue stockQueue() {
        return new Queue(RabbitMqConstant.STOCK_QUEUE_NAME);
    }

    @Bean(name = "orderDelayQueue")
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>(256);
        // 设置消息失效后发送给哪个交换机（死信）
        args.put("x-dead-letter-exchange", RabbitMqConstant.ORDER_EVENT_EXCHANGE);
        // 设置（死信）交换机的 Routing-key
        args.put("x-dead-letter-routing-key", RabbitMqConstant.ORDER_RELEASE_KEY);
        // 设置消息过期时间单位（ms）
        args.put("x-message-ttl", 60000);
        return new Queue(RabbitMqConstant.ORDER_DELAY_QUEUE, true, false, false, args);
    }

    @Bean(name = "orderReleaseQueue")
    public Queue orderReleaseQueue() {
        return new Queue(RabbitMqConstant.ORDER_RELEASE_QUEUE);
    }

}
