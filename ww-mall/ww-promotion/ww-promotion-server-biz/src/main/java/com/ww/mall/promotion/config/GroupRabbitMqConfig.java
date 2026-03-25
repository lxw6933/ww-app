package com.ww.mall.promotion.config;

import com.ww.mall.promotion.mq.GroupMqConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2025-12-08 18:20
 * @description: 拼团RabbitMQ配置
 */
@Slf4j
@Configuration
public class GroupRabbitMqConfig {

    /**
     * 拼团交换机
     */
    @Bean
    public TopicExchange groupExchange() {
        return new TopicExchange(GroupMqConstant.GROUP_EXCHANGE, true, false);
    }

    /**
     * 支付成功驱动拼团队列。
     */
    @Bean
    public Queue groupOrderPaidQueue() {
        return QueueBuilder.durable(GroupMqConstant.GROUP_ORDER_PAID_QUEUE).build();
    }

    /**
     * 售后成功驱动拼团回退名额队列。
     */
    @Bean
    public Queue groupAfterSaleQueue() {
        return QueueBuilder.durable(GroupMqConstant.GROUP_AFTER_SALE_QUEUE).build();
    }

    /**
     * 拼团状态变更内部驱动队列。
     */
    @Bean
    public Queue groupStateChangedQueue() {
        return QueueBuilder.durable(GroupMqConstant.GROUP_STATE_CHANGED_QUEUE).build();
    }

    /**
     * 绑定支付成功驱动拼团队列。
     */
    @Bean
    public Binding groupOrderPaidBinding() {
        return BindingBuilder.bind(groupOrderPaidQueue())
                .to(groupExchange())
                .with(GroupMqConstant.GROUP_ORDER_PAID_KEY);
    }

    /**
     * 绑定售后成功驱动拼团回退队列。
     */
    @Bean
    public Binding groupAfterSaleBinding() {
        return BindingBuilder.bind(groupAfterSaleQueue())
                .to(groupExchange())
                .with(GroupMqConstant.GROUP_AFTER_SALE_KEY);
    }

    /**
     * 绑定拼团状态变更内部驱动队列。
     */
    @Bean
    public Binding groupStateChangedBinding() {
        return BindingBuilder.bind(groupStateChangedQueue())
                .to(groupExchange())
                .with(GroupMqConstant.GROUP_STATE_CHANGED_KEY);
    }

}
