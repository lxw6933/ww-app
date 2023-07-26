package com.ww.mall.rabbitmq.bind;

import com.ww.mall.rabbitmq.MallRabbitmqAutoConfiguration;
import com.ww.mall.rabbitmq.routekey.RouteKeyConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/21 23:04
 **/
@Configuration
@ConditionalOnBean(MallRabbitmqAutoConfiguration.class)
public class MallMemberBindingConfiguration {

    @Resource
    private TopicExchange mallMemberExchange;

    @Resource
    private TopicExchange mallCanalExchange;

    @Resource
    private Queue mallMemberRegisterQueue;


    @Resource
    private Queue mallCanalQueue;

    @Bean
    public Binding mallMemberRegisterBinding() {
        return BindingBuilder.bind(mallMemberRegisterQueue)
                .to(mallMemberExchange)
                .with(RouteKeyConstant.MALL_MEMBER_REGISTER_KEY);
    }

    @Bean
    public Binding mallCanalBinding() {
        return BindingBuilder.bind(mallCanalQueue)
                .to(mallCanalExchange)
                .with(RouteKeyConstant.MALL_CANAL_KEY);
    }

}
