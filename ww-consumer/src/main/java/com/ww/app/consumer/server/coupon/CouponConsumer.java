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

    @PostConstruct
    public void init() {
        System.out.println("初始化CouponConsumer消费者");
    }

    @RabbitListener(queues = {QueueConstant.COUPON_TEST_QUEUE})
    public void memberRegisterMessage(Message message, Integer msg, Channel channel) throws IOException {
        log.info("收到mall_coupon服务发送traceId测试的消息：{}", msg);
        MsgConsumerTemplate<Integer> couponTraceIdTestMsgConsumer = new CouponTraceIdTestMsgConsumerTemplate();
        couponTraceIdTestMsgConsumer.consumer(message, msg, channel);
    }

}
