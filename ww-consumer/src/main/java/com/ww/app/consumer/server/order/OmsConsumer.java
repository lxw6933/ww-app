package com.ww.app.consumer.server.order;

import com.rabbitmq.client.Channel;
import com.ww.app.common.utils.json.JacksonUtils;
import com.ww.app.rabbitmq.queue.QueueConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
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
    public void omsCloseMessage(Channel channel, List<Message> msgList) throws IOException {
        System.out.println("=======================================");
        for (int i = 0; i < msgList.size(); i++) {
            Message message = msgList.get(i);
            String msg = JacksonUtils.parseObject(message.getBody(), String.class);
            MessageProperties messageProperties = message.getMessageProperties();
            long tag = messageProperties.getDeliveryTag();
            if (i % 2 == 1) {
                System.out.println("消费失败[" + msg + "] tag:[" + tag + "]");
                channel.basicNack(tag, false, true);
            } else {
                System.out.println("消费成功[" + msg + "] tag:[" + tag + "]");
                channel.basicAck(tag, false);
            }
        }
        System.out.println("=======================================");
        // 没有异常，会自动全部ack消息
//        throw new ApiException("异常");
    }

}
