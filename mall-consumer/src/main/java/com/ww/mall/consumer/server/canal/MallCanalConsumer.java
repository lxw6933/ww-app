package com.ww.mall.consumer.server.canal;

import com.rabbitmq.client.Channel;
import com.ww.mall.rabbitmq.queue.QueueConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/26 22:37
 **/
@Slf4j
@Component
public class MallCanalConsumer {

    @RabbitListener(queues = {QueueConstant.MALL_CANAL_QUEUE_NAME})
    public void memberRegisterMessage(Message message, String msg, Channel channel) throws IOException {
        log.info("收到canal的消息：{}", msg);
    }

}
