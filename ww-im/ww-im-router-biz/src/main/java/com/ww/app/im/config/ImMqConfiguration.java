package com.ww.app.im.config;

import com.ww.app.im.router.api.common.ImRouterMqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
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

    @Bean
    public DirectRabbitListenerContainerFactory imBatchContainerFactory(ConnectionFactory connectionFactory, MessageConverter converter) {
        DirectRabbitListenerContainerFactory factory = new DirectRabbitListenerContainerFactory();
        // 设置连接工厂
        factory.setConnectionFactory(connectionFactory);
        // 设置每个队列的消费者数量
        factory.setConsumersPerQueue(10);
        // 设置每个消费者的预取消息数量
        factory.setPrefetchCount(10);
        // 设置消息转换器
        factory.setMessageConverter(converter);
        return factory;
    }

}
