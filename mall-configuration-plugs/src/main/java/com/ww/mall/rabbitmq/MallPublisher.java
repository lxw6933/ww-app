package com.ww.mall.rabbitmq;

import cn.hutool.extra.spring.SpringUtil;
import com.ww.mall.rabbitmq.enums.MqMsgStatus;
import com.ww.mall.rabbitmq.repository.BaseMqLog;
import com.ww.mall.rabbitmq.repository.MqLogRepository;
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

    @SuppressWarnings("unchecked")
    private final MqLogRepository<String, BaseMqLog> mqLogRepository = SpringUtil.getBean(MqLogRepository.class);

    public <T> void publishMsg(String exchange, String routeKey, T msg) {
        // 自定义消息id
        MallCorrelationData<T> msgData = new MallCorrelationData<>();
        msgData.setExchange(exchange);
        msgData.setMessage(msg);
        msgData.setRetryCount(0);
        msgData.setRoutingKey(routeKey);
        msgData.setId(UUID.randomUUID().toString().replaceAll("-", ""));
        // 保存消息
        mqLogRepository.save(msgData, MqMsgStatus.DELIVER_SUCCESS);
        // 发送用户对象信息到broker
        rabbitTemplate.convertAndSend(exchange, routeKey, msg, correlationIdProcessor, msgData);
    }


}
