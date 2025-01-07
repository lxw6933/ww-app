package com.ww.app.rabbitmq;

import com.ww.app.rabbitmq.common.MyCorrelationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2023-12-13- 18:18
 * @description:
 */
@Slf4j
@Component
public class RabbitMqPublisher {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private MessagePostProcessor correlationIdProcessor;

    public <T> void sendMsg(String exchange, String routeKey, T msg) {
        sendDelayMsg(exchange, routeKey, msg, 0);
    }

    /**
     * 延迟消息发送
     *
     * @param delayTime 延时时长 【单位：秒】
     */
    public <T> void sendDelayMsg(String exchange, String routeKey, T msg, int delayTime) {
        // 自定义消息id
        MyCorrelationData<T> msgData = new MyCorrelationData<>(true);
        msgData.setExchange(exchange);
        msgData.setMessage(msg);
        msgData.setRoutingKey(routeKey);
        msgData.setDelayTime(delayTime);
        rabbitTemplate.convertAndSend(exchange, routeKey, msg, correlationIdProcessor, msgData);
    }

}
