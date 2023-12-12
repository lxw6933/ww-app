package com.ww.mall.rabbitmq.bind;

import com.ww.mall.rabbitmq.exchange.ExchangeConstant;
import com.ww.mall.rabbitmq.queue.QueueConstant;
import com.ww.mall.rabbitmq.routekey.RouteKeyConstant;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;

import javax.annotation.Resource;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/21 23:04
 **/
public class MallBindingConfiguration {

    /**
     * 通用定制延时队列
     */
    @Resource(name = ExchangeConstant.MALL_COMMON_DELAY_EXCHANGE)
    private CustomExchange mallCommonDelayExchange;

    /**
     * coupon exchange
     */
    @Resource(name = ExchangeConstant.MALL_COUPON_EXCHANGE)
    private TopicExchange mallCouponExchange;

    /**
     * member exchange
     */
    @Resource(name = ExchangeConstant.MALL_MEMBER_EXCHANGE)
    private TopicExchange mallMemberExchange;

    /**
     * canal exchange
     */
    @Resource(name = ExchangeConstant.MALL_CANAL_EXCHANGE)
    private TopicExchange mallCanalExchange;

    /**
     * oms exchange
     */
    @Resource(name = ExchangeConstant.MALL_OMS_EXCHANGE)
    private TopicExchange mallOmsExchange;

    @Resource(name = QueueConstant.MALL_COUPON_TEST_QUEUE)
    private Queue mallCouponTestQueue;

    /**
     * 会员注册队列
     */
    @Resource(name = QueueConstant.MALL_MEMBER_REGISTER_QUEUE)
    private Queue mallMemberRegisterQueue;

    /**
     * 同步数据库数据队列
     */
    @Resource(name = QueueConstant.MALL_CANAL_QUEUE)
    private Queue mallCanalQueue;

    /**
     * 关单队列
     */
    @Resource(name = QueueConstant.MALL_OMS_CLOSE_QUEUE)
    private Queue omsCloseQueue;

    /**
     * 【固定】延时关单死信队列
     */
    @Resource(name = QueueConstant.MALL_OMS_DELAY_FIFTEEN_QUEUE)
    private Queue omsDelayFiftyQueue;

    /**
     * 商品定时上架队列
     */
    @Resource(name = QueueConstant.MALL_PRODUCT_TIMER_UP_QUEUE)
    private Queue productTimerUpQueue;

    @Bean
    public Binding mallCouponTestBinding() {
        return BindingBuilder.bind(mallCouponTestQueue).to(mallCouponExchange).with(RouteKeyConstant.MALL_COUPON_TEST_KEY);
    }

    @Bean
    public Binding mallMemberRegisterBinding() {
        return BindingBuilder.bind(mallMemberRegisterQueue).to(mallMemberExchange).with(RouteKeyConstant.MALL_MEMBER_REGISTER_KEY);
    }

    @Bean
    public Binding mallCanalBinding() {
        return BindingBuilder.bind(mallCanalQueue).to(mallCanalExchange).with(RouteKeyConstant.MALL_CANAL_KEY);
    }

    @Bean
    public Binding closeOrderBinding() {
        return BindingBuilder.bind(omsCloseQueue).to(mallOmsExchange).with(RouteKeyConstant.MALL_OMS_CLOSE_KEY);
    }

    @Bean
    public Binding omsDelayFiftyBinding() {
        return BindingBuilder.bind(omsDelayFiftyQueue).to(mallOmsExchange).with(RouteKeyConstant.MALL_OMS_DELAY_FIFTEEN_KEY);
    }

    @Bean
    public Binding productTimerUpBinding() {
        return BindingBuilder.bind(productTimerUpQueue).to(mallCommonDelayExchange).with(RouteKeyConstant.MALL_PRODUCT_TIMER_UP_KEY).noargs();
    }

}
