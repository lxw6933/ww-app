package com.ww.mall.consumer.server.coupon;

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
 * @create 2023-12-12- 17:57
 * @description:
 */
@Slf4j
@Component
@ConditionalOnBean(MallQueueConfiguration.class)
public class CouponConsumer {

    @RabbitListener(queues = {QueueConstant.MALL_COUPON_TEST_QUEUE})
    public void memberRegisterMessage(Message message, String msg, Channel channel) throws IOException {
        log.info("收到mall_coupon服务发送traceId测试的消息：{}", msg);
        MsgConsumerTemplate<String> couponTraceIdTestMsgConsumer = new CouponTraceIdTestMsgConsumerTemplate();
        couponTraceIdTestMsgConsumer.consumer(message, msg, channel);
    }

}
