package com.ww.mall.rabbitmq.exchange;

import com.ww.mall.rabbitmq.MallRabbitmqAutoConfiguration;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/21 22:59
 **/
@Configuration
@ConditionalOnBean(MallRabbitmqAutoConfiguration.class)
public class ExchangeConfiguration {

    @Bean
    public TopicExchange mallMemberExchange() {
        return new TopicExchange(ExchangeConstant.MALL_MEMBER_EXCHANGE_NAME);
    }

    @Bean
    public TopicExchange mallCanalExchange() {
        return new TopicExchange(ExchangeConstant.MALL_CANAL_EXCHANGE_NAME);
    }

}
