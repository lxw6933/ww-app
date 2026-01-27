package com.ww.app.consumer.server.coupon;

import com.rabbitmq.client.Channel;
import com.ww.app.rabbitmq.template.MsgConsumerTemplate;
import com.ww.app.rabbitmq.queue.QueueConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * @author ww
 * @create 2023-12-12- 17:57
 * @description:
 */
@Slf4j
@Component
public class CouponConsumer {

    private final MsgConsumerTemplate<Integer> couponTraceIdTestMsgConsumer;

    public CouponConsumer(CouponTraceIdTestMsgConsumerTemplate couponTraceIdTestMsgConsumer) {
        this.couponTraceIdTestMsgConsumer = couponTraceIdTestMsgConsumer;
    }

    @PostConstruct
    public void init() {
        System.out.println("初始化CouponConsumer消费者");
    }

    @RabbitListener(queues = {QueueConstant.COUPON_TEST_QUEUE}, containerFactory = "msgConsumerContainerFactory")
    public void memberRegisterMessage(Message message, Integer msg, Channel channel) throws IOException {
        log.info("MQ消费[queue={}] payload={}", QueueConstant.COUPON_TEST_QUEUE, msg);
        couponTraceIdTestMsgConsumer.consumer(message, msg, channel);
    }

}
