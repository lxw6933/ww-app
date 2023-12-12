package com.ww.mall.coupon.config;

import com.ww.mall.rabbitmq.MallCorrelationData;
import com.ww.mall.rabbitmq.exchange.ExchangeConstant;
import com.ww.mall.rabbitmq.routekey.RouteKeyConstant;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * @description:
 * @author: ww
 * @create: 2023/12/12 18:15
 **/
@Component
public class CouponPublisher {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private MessagePostProcessor correlationIdProcessor;

    /**
     * 发送traceId test消息
     *
     * @param msg msg
     */
    public void publishTestMsg(String msg) {
        // 自定义消息id
        MallCorrelationData msgData = new MallCorrelationData();
        msgData.setExchange(ExchangeConstant.MALL_COUPON_EXCHANGE);
        msgData.setMessage(msg);
        msgData.setRetryCount(0);
        msgData.setRoutingKey(RouteKeyConstant.MALL_COUPON_TEST_KEY);
        msgData.setId(UUID.randomUUID().toString());
        // 发送用户对象信息到broker
        rabbitTemplate.convertAndSend(
                ExchangeConstant.MALL_COUPON_EXCHANGE,
                RouteKeyConstant.MALL_COUPON_TEST_KEY,
                msg,
                correlationIdProcessor,
                msgData);
    }

}
