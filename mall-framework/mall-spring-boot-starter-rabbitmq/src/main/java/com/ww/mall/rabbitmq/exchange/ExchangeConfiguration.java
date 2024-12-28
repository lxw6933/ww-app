package com.ww.mall.rabbitmq.exchange;

import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/21 22:59
 **/
public class ExchangeConfiguration {

    @Bean(name = ExchangeConstant.CACHE_NOTICE_FANOUT_EXCHANGE)
    public FanoutExchange mallCacheNoticeFanoutExchange() {
        return new FanoutExchange(ExchangeConstant.CACHE_NOTICE_FANOUT_EXCHANGE);
    }

    @Bean(name = ExchangeConstant.COMMON_DELAY_EXCHANGE)
    public CustomExchange mallCommonDelayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "topic");
        return new CustomExchange(ExchangeConstant.COMMON_DELAY_EXCHANGE, "x-delayed-message",true, false, args);
    }

    @Bean(name = ExchangeConstant.COUPON_EXCHANGE)
    public TopicExchange mallCouponExchange() {
        return new TopicExchange(ExchangeConstant.COUPON_EXCHANGE);
    }

    @Bean(name = ExchangeConstant.MEMBER_EXCHANGE)
    public TopicExchange mallMemberExchange() {
        return new TopicExchange(ExchangeConstant.MEMBER_EXCHANGE);
    }

    @Bean(name = ExchangeConstant.CANAL_EXCHANGE)
    public TopicExchange mallCanalExchange() {
        return new TopicExchange(ExchangeConstant.CANAL_EXCHANGE);
    }

    @Bean(name = ExchangeConstant.OMS_EXCHANGE)
    public TopicExchange mallOmsExchange() {return new TopicExchange(ExchangeConstant.OMS_EXCHANGE);}

}
