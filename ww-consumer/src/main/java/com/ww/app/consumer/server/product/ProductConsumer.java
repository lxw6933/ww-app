package com.ww.app.consumer.server.product;

import com.rabbitmq.client.Channel;
import com.ww.app.rabbitmq.template.MsgConsumerTemplate;
import com.ww.app.rabbitmq.queue.QueueConstant;
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
public class ProductConsumer {

    private final MsgConsumerTemplate<Long> productTimerUpMsgConsumer;

    public ProductConsumer(ProductTimerUpMsgConsumerTemplate productTimerUpMsgConsumer) {
        this.productTimerUpMsgConsumer = productTimerUpMsgConsumer;
    }

    @RabbitListener(queues = {QueueConstant.PRODUCT_TIMER_UP_QUEUE}, containerFactory = "msgConsumerContainerFactory")
    public void memberRegisterMessage(Message message, Long productId, Channel channel) throws IOException {
        log.info("MQ消费[queue={}] payload={}", QueueConstant.PRODUCT_TIMER_UP_QUEUE, productId);
        productTimerUpMsgConsumer.consumer(message, productId, channel);
    }

}
