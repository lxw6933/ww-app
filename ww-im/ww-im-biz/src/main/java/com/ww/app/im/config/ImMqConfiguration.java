package com.ww.app.im.config;

import com.ww.app.im.api.common.ImBizMqConstant;
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

    @Bean(name = ImBizMqConstant.IM_BIZ_EXCHANGE)
    public TopicExchange imBizExchange() {
        return new TopicExchange(ImBizMqConstant.IM_BIZ_EXCHANGE);
    }

    @Bean(name = ImBizMqConstant.IM_BIZ_MSG_QUEUE)
    public Queue imBizMsgQueue() {
        return new Queue(ImBizMqConstant.IM_BIZ_MSG_QUEUE);
    }

    @Bean
    public Binding imBizMsgBinding() {
        return BindingBuilder.bind(imBizMsgQueue()).to(imBizExchange()).with(ImBizMqConstant.IM_BIZ_MSG_KEY);
    }

    @Bean(name = ImBizMqConstant.IM_BIZ_MSG_HANDLE_QUEUE)
    public Queue imBizMsgHandleQueue() {
        return new Queue(ImBizMqConstant.IM_BIZ_MSG_HANDLE_QUEUE);
    }

    @Bean
    public Binding imBizMsgHandleBinding() {
        return BindingBuilder.bind(imBizMsgHandleQueue()).to(imBizExchange()).with(ImBizMqConstant.IM_BIZ_MSG_HANDLE_KEY);
    }

}
