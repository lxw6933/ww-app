package com.ww.app.redpacket.consumer;

import com.rabbitmq.client.Channel;
import com.ww.app.redpacket.common.RedpacketMQConstant;
import com.ww.app.redpacket.consumer.template.RedpacketRecordMsgConsumerTemplate;
import com.ww.app.redpacket.consumer.template.RedpacketRollbackMsgConsumerTemplate;
import com.ww.app.redpacket.dto.RedpacketReceiveDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @author ww
 * @create 2024-12-30- 15:58
 * @description: 红包数据消费者
 */
@Slf4j
@Component
public class RedpacketConsumer {

    @Resource
    private RedpacketRollbackMsgConsumerTemplate redpacketRollbackMsgConsumerTemplate;

    @Resource
    private RedpacketRecordMsgConsumerTemplate redpacketRecordMsgConsumerTemplate;

    /**
     * 红包回滚监听器
     */
    @RabbitListener(queues = {RedpacketMQConstant.REDPACKET_ROLLBACK_QUEUE})
    public void redpacketRollbackListener(Message message, Channel channel, String redpacketId) throws IOException {
        redpacketRollbackMsgConsumerTemplate.consumer(message, redpacketId, channel);
    }

    /**
     * 红包领取监听器
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = RedpacketMQConstant.REDPACKET_RECORD_QUEUE, durable = "true"),
            exchange = @Exchange(value = RedpacketMQConstant.REDPACKET_EXCHANGE),
            key = RedpacketMQConstant.REDPACKET_RECORD_KEY
    ))
    public void redpacketReceiveListener(Message message, Channel channel, RedpacketReceiveDTO redpacketReceiveDTO) throws IOException {
        redpacketRecordMsgConsumerTemplate.consumer(message, redpacketReceiveDTO, channel);
    }

}
