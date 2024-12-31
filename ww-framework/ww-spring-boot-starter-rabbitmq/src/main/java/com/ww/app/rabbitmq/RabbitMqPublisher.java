package com.ww.app.rabbitmq;

import com.ww.app.rabbitmq.common.BaseMqLog;
import com.ww.app.rabbitmq.common.MyCorrelationData;
import com.ww.app.common.enums.MqMsgStatus;
import com.ww.app.rabbitmq.repository.MqLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
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

    @Resource
    private MqLogRepository<String, BaseMqLog> mqLogRepository;

    public <T> void publishMsg(String exchange, String routeKey, T msg) {
        this.publishMsg(exchange, routeKey, msg, true);
    }

    public <T> void publishSimpleMsg(String exchange, String routeKey, T msg) {
        this.publishMsg(exchange, routeKey, msg, false);
    }

    private <T> void publishMsg(String exchange, String routeKey, T msg, boolean msgMode) {
        // 自定义消息id
        MyCorrelationData<T> msgData = new MyCorrelationData<>();
        msgData.setExchange(exchange);
        msgData.setMessage(msg);
        msgData.setRoutingKey(routeKey);
        msgData.setMsgMode(msgMode);
        if (msgMode) {
            // 保存消息
            mqLogRepository.save(msgData, MqMsgStatus.DELIVER_SUCCESS);
        }
        // 发送用户对象信息到broker
        rabbitTemplate.convertAndSend(exchange, routeKey, msg, correlationIdProcessor, msgData);
    }

    public <T> void sendDelayMsg(String exchange, String routeKey, T msg, int delayTime) {
        sendDelayMsg(exchange, routeKey, msg, delayTime, false);
    }

    /**
     * 延迟消息发送
     * @param delayTime 延时时长 【单位：秒】
     */
    public <T> void sendDelayMsg(String exchange, String routeKey, T msg, int delayTime, boolean msgMode) {
        // 自定义消息id
        MyCorrelationData<T> msgData = new MyCorrelationData<>();
        msgData.setExchange(exchange);
        msgData.setMessage(msg);
        msgData.setRoutingKey(routeKey);
        msgData.setMsgMode(msgMode);
        rabbitTemplate.convertAndSend(exchange, routeKey, msg, message -> {
            // 消息持久化
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            // 单位为毫秒
            int time = delayTime * 1000;
            message.getMessageProperties().setDelay(time);
            return message;
        }, msgData);
    }

}
