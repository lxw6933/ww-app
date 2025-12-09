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
     * 拼团成功队列
     */
    @Bean
    public Queue groupSuccessQueue() {
        return QueueBuilder.durable(GroupMqConstant.GROUP_SUCCESS_QUEUE).build();
    }

    /**
     * 拼团失败队列
     */
    @Bean
    public Queue groupFailedQueue() {
        return QueueBuilder.durable(GroupMqConstant.GROUP_FAILED_QUEUE).build();
    }

    /**
     * 拼团过期队列
     */
    @Bean
    public Queue groupExpiredQueue() {
        return QueueBuilder.durable(GroupMqConstant.GROUP_EXPIRED_QUEUE).build();
    }

    /**
     * 拼团退款队列
     */
    @Bean
    public Queue groupRefundQueue() {
        return QueueBuilder.durable(GroupMqConstant.GROUP_REFUND_QUEUE).build();
    }

    /**
     * 绑定拼团成功队列
     */
    @Bean
    public Binding groupSuccessBinding() {
        return BindingBuilder.bind(groupSuccessQueue())
                .to(groupExchange())
                .with(GroupMqConstant.GROUP_SUCCESS_KEY);
    }

    /**
     * 绑定拼团失败队列
     */
    @Bean
    public Binding groupFailedBinding() {
        return BindingBuilder.bind(groupFailedQueue())
                .to(groupExchange())
                .with(GroupMqConstant.GROUP_FAILED_KEY);
    }

    /**
     * 绑定拼团过期队列
     */
    @Bean
    public Binding groupExpiredBinding() {
        return BindingBuilder.bind(groupExpiredQueue())
                .to(groupExchange())
                .with(GroupMqConstant.GROUP_EXPIRED_KEY);
    }

    /**
     * 绑定拼团退款队列
     */
    @Bean
    public Binding groupRefundBinding() {
        return BindingBuilder.bind(groupRefundQueue())
                .to(groupExchange())
                .with(GroupMqConstant.GROUP_REFUND_KEY);
    }

}
