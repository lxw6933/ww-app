package com.ww.mall.rabbitmq;

import com.ww.mall.rabbitmq.common.BaseMqLog;
import com.ww.mall.rabbitmq.common.MallCorrelationData;
import com.ww.mall.common.enums.MqMsgStatus;
import com.ww.mall.rabbitmq.repository.MqLogRepository;
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
public class MallPublisher {

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
        MallCorrelationData<T> msgData = new MallCorrelationData<>();
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

}
