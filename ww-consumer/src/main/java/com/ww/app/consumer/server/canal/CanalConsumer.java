package com.ww.app.consumer.server.canal;

import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import com.ww.app.rabbitmq.template.MsgConsumerTemplate;
import com.ww.app.rabbitmq.queue.QueueConfiguration;
import com.ww.app.rabbitmq.queue.QueueConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/26 22:37
 **/
@Slf4j
@Component
@ConditionalOnBean(QueueConfiguration.class)
public class CanalConsumer {

    private final MsgConsumerTemplate<CanalMessage<?>> canalMsgConsumerTemplate;

    public CanalConsumer(CanalMsgConsumerTemplate canalMsgConsumerTemplate) {
        this.canalMsgConsumerTemplate = canalMsgConsumerTemplate;
    }

    @RabbitListener(queues = {QueueConstant.CANAL_QUEUE}, containerFactory = "msgConsumerContainerFactory")
    public void memberRegisterMessage(Message message, Channel channel) throws IOException {
        // 注意此处MQ传来的是Byte[]
        byte[] bodyByte = message.getBody();
        String bodyStr = new String(bodyByte, StandardCharsets.UTF_8);
        CanalMessage<?> canalMessage = JSONUtil.toBean(bodyStr, CanalMessage.class);
        log.info("MQ消费[queue={}] payload={}", QueueConstant.CANAL_QUEUE, canalMessage);
        canalMsgConsumerTemplate.consumer(message, canalMessage, channel);
    }

}
