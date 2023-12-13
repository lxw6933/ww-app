package com.ww.mall.consumer.server.order;

import com.rabbitmq.client.Channel;
import com.ww.mall.consumer.template.MsgConsumerTemplate;
import com.ww.mall.rabbitmq.queue.MallQueueConfiguration;
import com.ww.mall.rabbitmq.queue.QueueConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author ww
 * @create 2023-07-28- 09:09
 * @description:
 */
@Slf4j
@Component
public class MallOmsConsumer {

    @RabbitListener(queues = {QueueConstant.MALL_OMS_CLOSE_QUEUE})
    public void omsCloseMessage(Message message, Long mainOrderId, Channel channel) throws IOException {
        log.info("收到mall_oms服务发送订单关闭的消息：{}", mainOrderId);
        MsgConsumerTemplate<Long> omsCloseMsgConsumer = new OmsCloseMsgConsumerTemplate();
        omsCloseMsgConsumer.consumer(message, mainOrderId, channel);
    }

}
