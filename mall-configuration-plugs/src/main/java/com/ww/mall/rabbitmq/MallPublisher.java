package com.ww.mall.rabbitmq;

import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * @author ww
 * @create 2023-12-13- 18:18
 * @description:
 */
@Component
public class MallPublisher {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private MessagePostProcessor correlationIdProcessor;

    public <T> void publishMsg(String exchange, String routeKey, T msg) {
        // 自定义消息id
        MallCorrelationData<T> msgData = new MallCorrelationData<>();
        msgData.setExchange(exchange);
        msgData.setMessage(msg);
        msgData.setRetryCount(0);
        msgData.setRoutingKey(routeKey);
        msgData.setId(UUID.randomUUID().toString());
        // 发送用户对象信息到broker
        rabbitTemplate.convertAndSend(exchange, routeKey, msg, correlationIdProcessor, msgData);
    }


}
