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

    @Bean(name = ExchangeConstant.MALL_MEMBER_EXCHANGE)
    public TopicExchange mallMemberExchange() {
        return new TopicExchange(ExchangeConstant.MALL_MEMBER_EXCHANGE);
    }

    @Bean(name = ExchangeConstant.MALL_CANAL_EXCHANGE)
    public TopicExchange mallCanalExchange() {
        return new TopicExchange(ExchangeConstant.MALL_CANAL_EXCHANGE);
    }

    @Bean(name = ExchangeConstant.MALL_OMS_EXCHANGE)
    public TopicExchange mallOmsExchange() {return new TopicExchange(ExchangeConstant.MALL_OMS_EXCHANGE);}

}
