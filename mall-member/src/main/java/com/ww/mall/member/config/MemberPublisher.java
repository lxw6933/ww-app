package com.ww.mall.member.config;

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
 * @create: 2023/7/21 23:15
 **/
@Component
public class MemberPublisher {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private MessagePostProcessor correlationIdProcessor;

    /**
     * 发送用户注册消息
     *
     * @param memberId 用户id
     */
    public void publishMemberRegisterMsg(Long memberId) {
        // 自定义消息id
        MallCorrelationData msgData = new MallCorrelationData();
        msgData.setExchange(ExchangeConstant.MALL_MEMBER_EXCHANGE);
        msgData.setMessage(memberId);
        msgData.setRetryCount(0);
        msgData.setRoutingKey(RouteKeyConstant.MALL_MEMBER_REGISTER_KEY);
        msgData.setId(UUID.randomUUID().toString());
        // 发送用户对象信息到broker
        rabbitTemplate.convertAndSend(
                ExchangeConstant.MALL_MEMBER_EXCHANGE,
                RouteKeyConstant.MALL_MEMBER_REGISTER_KEY,
                memberId,
                correlationIdProcessor,
                msgData);
    }

}
