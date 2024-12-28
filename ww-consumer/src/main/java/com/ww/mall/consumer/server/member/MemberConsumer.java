package com.ww.mall.consumer.server.member;

import com.rabbitmq.client.Channel;
import com.ww.mall.rabbitmq.template.MsgConsumerTemplate;
import com.ww.mall.rabbitmq.queue.QueueConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/22 11:07
 **/
@Slf4j
@Component
public class MemberConsumer {

    @RabbitListener(queues = {QueueConstant.MEMBER_REGISTER_QUEUE})
    public void memberRegisterMessage(Message message, Long memberId, Channel channel) throws IOException {
        MsgConsumerTemplate<Long> memberRegisterMsgConsumer = new MemberRegisterMsgConsumerTemplate();
        memberRegisterMsgConsumer.consumer(message, memberId, channel);
    }

}
