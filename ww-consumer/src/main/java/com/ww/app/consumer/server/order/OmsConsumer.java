package com.ww.app.consumer.server.order;

import com.rabbitmq.client.Channel;
import com.ww.app.common.exception.ApiException;
import com.ww.app.rabbitmq.queue.QueueConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * @author ww
 * @create 2023-07-28- 09:09
 * @description:
 */
@Slf4j
@Component
public class OmsConsumer {

    @RabbitListener(queues = {QueueConstant.OMS_CLOSE_QUEUE}, containerFactory = "batchContainerFactory")
    public void omsCloseMessage(List<Message> messages, Channel channel) throws IOException {
        log.info("收到mall_oms服务发送订单关闭的消息数量：{}", messages.size());
        messages.forEach(message -> {
            System.out.println("消费:" + new String(message.getBody()));
        });
        // 没有异常，会自动全部ack消息
        throw new ApiException("异常");
    }

}
