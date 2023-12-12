package com.ww.mall.rabbitmq.exchange;

import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/21 22:59
 **/
public class MallExchangeConfiguration {

    @Bean(name = ExchangeConstant.MALL_COMMON_DELAY_EXCHANGE)
    public CustomExchange mallCommonDelayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "topic");
        return new CustomExchange(ExchangeConstant.MALL_COMMON_DELAY_EXCHANGE, "x-delayed-message",true, false, args);
    }

    @Bean(name = ExchangeConstant.MALL_COUPON_EXCHANGE)
    public TopicExchange mallCouponExchange() {
        return new TopicExchange(ExchangeConstant.MALL_COUPON_EXCHANGE);
    }

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
