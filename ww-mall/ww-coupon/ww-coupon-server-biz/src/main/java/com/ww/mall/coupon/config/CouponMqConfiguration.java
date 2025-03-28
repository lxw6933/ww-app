package com.ww.mall.coupon.config;

import com.ww.mall.coupon.constant.CouponMqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2025-03-28- 15:41
 * @description:
 */
@Configuration
public class CouponMqConfiguration {

    @Bean(name = CouponMqConstant.COUPON_BIZ_EXCHANGE)
    public TopicExchange couponBizExchange() {
        return new TopicExchange(CouponMqConstant.COUPON_BIZ_EXCHANGE);
    }

    @Bean(name = CouponMqConstant.COUPON_BIZ_CODE_MSG_QUEUE)
    public Queue codeMsgQueue() {
        return new Queue(CouponMqConstant.COUPON_BIZ_CODE_MSG_QUEUE);
    }

    @Bean
    public Binding codeMsgBinding() {
        return BindingBuilder.bind(codeMsgQueue()).to(couponBizExchange()).with(CouponMqConstant.COUPON_BIZ_CODE_MSG_KEY);
    }

}
