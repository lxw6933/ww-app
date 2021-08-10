package com.ww.mall.config.rabbitmq.binding;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * @description:
 * @author: ww
 * @create: 2021/6/29 下午8:45
 **/
@Configuration
public class BindingConfig {

    @Resource
    private Queue infoQueue;

    @Resource
    private Queue stockQueue;

    @Resource
    private Queue demoQueue;

    @Resource
    private Queue orderDelayQueue;

    @Resource
    private Queue orderReleaseQueue;

    @Resource
    private TopicExchange mailExchange;

    @Resource
    private TopicExchange demoExchange;

    @Resource
    private TopicExchange orderEventExchange;

    @Bean
    public Binding mailBinding() {
        return BindingBuilder.bind(infoQueue).to(mailExchange).with(RabbitMqConstant.INFO_ROUTING_KEY);
    }

    @Bean
    public Binding demoBinding() {
        return BindingBuilder.bind(demoQueue).to(demoExchange).with(RabbitMqConstant.DEMO_ROUTING_KEY);
    }

    /**
     * 绑定dead-letter
     */
    @Bean
    public Binding orderDeadBinding() {
        return BindingBuilder.bind(orderDelayQueue).to(orderEventExchange).with(RabbitMqConstant.ORDER_CREATE_KEY);
    }
    @Bean
    public Binding orderReleaseBinding() {
        return BindingBuilder.bind(orderReleaseQueue).to(orderEventExchange).with(RabbitMqConstant.ORDER_RELEASE_KEY);
    }

}
