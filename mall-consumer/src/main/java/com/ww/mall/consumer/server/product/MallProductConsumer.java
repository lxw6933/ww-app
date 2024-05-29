package com.ww.mall.consumer.server.product;

import com.rabbitmq.client.Channel;
import com.ww.mall.rabbitmq.template.MsgConsumerTemplate;
import com.ww.mall.rabbitmq.queue.QueueConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author ww
 * @create 2023-07-28- 09:06
 * @description:
 */
@Slf4j
@Component
public class MallProductConsumer {

    @RabbitListener(queues = {QueueConstant.MALL_PRODUCT_TIMER_UP_QUEUE})
    public void memberRegisterMessage(Message message, Long productId, Channel channel) throws IOException {
        log.info("收到mall_product服务发送商品定时上架的消息：{}", productId);
        MsgConsumerTemplate<Long> productTimerUpMsgConsumer = new ProductTimerUpMsgConsumerTemplate();
        productTimerUpMsgConsumer.consumer(message, productId, channel);
    }

}
