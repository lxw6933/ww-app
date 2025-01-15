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
 * @description: rabbitmq消息发送者
 */
@Slf4j
@Component
public class RabbitMqPublisher {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private MessagePostProcessor correlationIdProcessor;

    /**
     * 使用默认交换机发送到队列【没有声明绑定关系的队列】
     *
     * @param queueName 队列名称
     * @param msg 消息
     * @param <T> 消息类型
     */
    public <T> void sendMsg(String queueName, T msg) {
        // 自定义消息id
        MyCorrelationData<T> msgData = new MyCorrelationData<>(true);
        msgData.setExchange(rabbitTemplate.getExchange());
        msgData.setMessage(msg);
        msgData.setRoutingKey(queueName);
        rabbitTemplate.convertAndSend(queueName, msg, correlationIdProcessor, msgData);
    }

    /**
     * 使用指定交换机发送到队列【声明了绑定关系的队列】
     *
     * @param exchange 交换机【TopicExchange】
     * @param routeKey 路由key
     * @param msg 消息
     * @param <T> 消息类型
     */
    public <T> void sendMsg(String exchange, String routeKey, T msg) {
        sendDelayMsg(exchange, routeKey, msg, 0);
    }

    /**
     * 使用自定义交换机发送到队列【声明了绑定关系的队列】
     *
     * @param exchange 自定义交换机【CustomExchange】
     * @param routeKey 路由key
     * @param msg 消息
     * @param delayTime 延时时长 【单位：秒】
     * @param <T> 消息类型
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
