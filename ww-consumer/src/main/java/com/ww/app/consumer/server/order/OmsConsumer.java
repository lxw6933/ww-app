package com.ww.app.consumer.server.order;

import com.rabbitmq.client.Channel;
import com.ww.app.common.utils.json.JacksonUtils;
import com.ww.app.rabbitmq.template.BatchMsgConsumerTemplate;
import com.ww.app.rabbitmq.queue.QueueConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ww
 * @create 2023-07-28- 09:09
 * @description:
 */
@Slf4j
@Component
public class OmsConsumer {

    private final BatchMsgConsumerTemplate<String> omsCloseBatchMsgConsumer;

    public OmsConsumer(OmsCloseBatchMsgConsumerTemplate omsCloseBatchMsgConsumer) {
        this.omsCloseBatchMsgConsumer = omsCloseBatchMsgConsumer;
    }

    @RabbitListener(queues = {QueueConstant.OMS_CLOSE_QUEUE}, containerFactory = "batchMsgConsumerContainerFactory")
    public void omsCloseMessage(Channel channel, List<Message> msgList) throws IOException {
        log.info("MQ消费[queue={}] payloadSize={}", QueueConstant.OMS_CLOSE_QUEUE, msgList.size());
        List<String> payloads = new ArrayList<>(msgList.size());
        for (Message message : msgList) {
            payloads.add(JacksonUtils.parseObject(message.getBody(), String.class));
        }
        omsCloseBatchMsgConsumer.batchConsumer(msgList, payloads, channel);
    }

}
