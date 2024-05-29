package com.ww.mall.consumer.server.canal;

import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import com.ww.mall.rabbitmq.template.MsgConsumerTemplate;
import com.ww.mall.rabbitmq.queue.MallQueueConfiguration;
import com.ww.mall.rabbitmq.queue.QueueConstant;
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
@ConditionalOnBean(MallQueueConfiguration.class)
public class MallCanalConsumer {

    @RabbitListener(queues = {QueueConstant.MALL_CANAL_QUEUE})
    public void memberRegisterMessage(Message message, Channel channel) throws IOException {
        // 注意此处MQ传来的是Byte[]
        byte[] bodyByte = message.getBody();
        String bodyStr = new String(bodyByte, StandardCharsets.UTF_8);
        String bodyJsonStr = JSONUtil.toJsonStr(bodyStr);
        CanalMessage<?> canalMessage = JSONUtil.toBean(bodyJsonStr, CanalMessage.class);
        log.info("收到canal消息,数据库:【{}】表名:【{}】操作类型:【{}】数据：【{}】", canalMessage.getDatabase(), canalMessage.getTable(), canalMessage.getType(), canalMessage.getData());
        MsgConsumerTemplate<CanalMessage<?>> canalMsgConsumer = new CanalMsgConsumerTemplate();
        canalMsgConsumer.consumer(message, canalMessage, channel);
    }

}
