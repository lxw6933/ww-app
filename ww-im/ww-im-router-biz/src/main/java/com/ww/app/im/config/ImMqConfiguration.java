package com.ww.app.im.config;

import com.ww.app.im.router.api.common.ImRouterMqConstant;
import com.ww.app.rabbitmq.queue.QueueConstant;
import com.ww.app.rabbitmq.routekey.RouteKeyConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2025-01-14- 15:48
 * @description:
 */
@Configuration
public class ImMqConfiguration {

    @Bean(name = ImRouterMqConstant.IM_ROUTER_EXCHANGE)
    public TopicExchange imRouterExchange() {
        return new TopicExchange(ImRouterMqConstant.IM_ROUTER_EXCHANGE);
    }

    @Bean(name = ImRouterMqConstant.IM_ROUTER_MSG_QUEUE)
    public Queue imRouterMsgQueue() {
        return new Queue(ImRouterMqConstant.IM_ROUTER_MSG_QUEUE);
    }

    @Bean
    public Binding imRouterMsgBinding() {
        return BindingBuilder.bind(imRouterMsgQueue()).to(imRouterExchange()).with(ImRouterMqConstant.IM_ROUTER_MSG_KEY);
    }

}
