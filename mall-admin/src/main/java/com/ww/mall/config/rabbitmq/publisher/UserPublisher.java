package com.ww.mall.config.rabbitmq.publisher;

import com.ww.mall.config.rabbitmq.MyCorrelationData;
import com.ww.mall.config.rabbitmq.binding.RabbitMqConstant;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * @description: 用户信息发布者
 * @author: ww
 * @create: 2021/6/29 下午10:22
 **/
@Component
public class UserPublisher {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private MessagePostProcessor correlationIdProcessor;

    /**
     * 将用户信息放入broker中
     *
     * @param demo 用户对象
     */
    public void publishDemoMsg(Object demo) {
        String msgId = UUID.randomUUID().toString();
        // 自定义消息id
        MyCorrelationData messageId = new MyCorrelationData();
        messageId.setExchange(RabbitMqConstant.DEMO_EXCHANGE_NAME);
        messageId.setMessage(demo);
        messageId.setRetryCount(0);
        messageId.setRoutingKey(RabbitMqConstant.DEMO_ROUTING_KEY);
        messageId.setId(msgId);
        // 发送用户对象信息到broker
        rabbitTemplate.convertAndSend(
                RabbitMqConstant.DEMO_EXCHANGE_NAME,
                RabbitMqConstant.DEMO_ROUTING_KEY,
                demo,
                correlationIdProcessor,
                messageId);
    }

}
