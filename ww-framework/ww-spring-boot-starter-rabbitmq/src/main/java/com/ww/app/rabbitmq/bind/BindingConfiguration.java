package com.ww.app.rabbitmq.bind;

import com.ww.app.rabbitmq.exchange.ExchangeConstant;
import com.ww.app.rabbitmq.queue.QueueConstant;
import com.ww.app.rabbitmq.routekey.RouteKeyConstant;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;

import javax.annotation.Resource;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/21 23:04
 **/
public class BindingConfiguration {

    /**
     * 广播交换机-缓存通知
     */
    @Resource(name = ExchangeConstant.CACHE_NOTICE_FANOUT_EXCHANGE)
    private FanoutExchange mallCacheNoticeFanoutExchange;

    /**
     * 通用定制延时队列
     */
    @Resource(name = ExchangeConstant.COMMON_DELAY_EXCHANGE)
    private CustomExchange mallCommonDelayExchange;

    /**
     * coupon exchange
     */
    @Resource(name = ExchangeConstant.COUPON_EXCHANGE)
    private TopicExchange mallCouponExchange;

    /**
     * member exchange
     */
    @Resource(name = ExchangeConstant.MEMBER_EXCHANGE)
    private TopicExchange mallMemberExchange;

    /**
     * canal exchange
     */
    @Resource(name = ExchangeConstant.CANAL_EXCHANGE)
    private TopicExchange mallCanalExchange;

    /**
     * 缓存通知队列
     */
    @Resource(name = QueueConstant.CACHE_NOTICE_QUEUE)
    private Queue mallCacheNoticeQueue;

    /**
     * oms exchange
     */
    @Resource(name = ExchangeConstant.OMS_EXCHANGE)
    private TopicExchange mallOmsExchange;

    @Resource(name = QueueConstant.COUPON_TEST_QUEUE)
    private Queue mallCouponTestQueue;

    @Resource(name = QueueConstant.CREATE_ORDER_QUEUE)
    private Queue mallCreateOrderQueue;

    /**
     * 会员注册队列
     */
    @Resource(name = QueueConstant.MEMBER_REGISTER_QUEUE)
    private Queue mallMemberRegisterQueue;

    /**
     * 同步数据库数据队列
     */
    @Resource(name = QueueConstant.CANAL_QUEUE)
    private Queue mallCanalQueue;

    /**
     * 关单队列
     */
    @Resource(name = QueueConstant.OMS_CLOSE_QUEUE)
    private Queue omsCloseQueue;

    /**
     * 【固定】延时关单死信队列
     */
    @Resource(name = QueueConstant.OMS_DELAY_FIFTEEN_QUEUE)
    private Queue omsDelayFiftyQueue;

    /**
     * 商品定时上架队列
     */
    @Resource(name = QueueConstant.PRODUCT_TIMER_UP_QUEUE)
    private Queue productTimerUpQueue;

    @Bean
    public Binding createOrderBinding() {
        return BindingBuilder.bind(mallCreateOrderQueue).to(mallOmsExchange).with(RouteKeyConstant.CREATE_ORDER_KEY);
    }

    @Bean
    public Binding mallCouponTestBinding() {
        return BindingBuilder.bind(mallCouponTestQueue).to(mallCouponExchange).with(RouteKeyConstant.COUPON_TEST_KEY);
    }

    @Bean
    public Binding mallMemberRegisterBinding() {
        return BindingBuilder.bind(mallMemberRegisterQueue).to(mallMemberExchange).with(RouteKeyConstant.MEMBER_REGISTER_KEY);
    }

    @Bean
    public Binding mallCanalBinding() {
        return BindingBuilder.bind(mallCanalQueue).to(mallCanalExchange).with(RouteKeyConstant.CANAL_KEY);
    }

    @Bean
    public Binding closeOrderBinding() {
        return BindingBuilder.bind(omsCloseQueue).to(mallOmsExchange).with(RouteKeyConstant.OMS_CLOSE_KEY);
    }

    @Bean
    public Binding omsDelayFiftyBinding() {
        return BindingBuilder.bind(omsDelayFiftyQueue).to(mallOmsExchange).with(RouteKeyConstant.OMS_DELAY_FIFTEEN_KEY);
    }

    @Bean
    public Binding productTimerUpBinding() {
        return BindingBuilder.bind(productTimerUpQueue).to(mallCommonDelayExchange).with(RouteKeyConstant.PRODUCT_TIMER_UP_KEY).noargs();
    }

    @Bean
    public Binding cacheNoticeBinding() {
        return BindingBuilder.bind(mallCacheNoticeQueue).to(mallCacheNoticeFanoutExchange);
    }

}
