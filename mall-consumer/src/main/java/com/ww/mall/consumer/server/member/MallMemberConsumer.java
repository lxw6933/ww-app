package com.ww.mall.consumer.server.member;

import com.rabbitmq.client.Channel;
import com.ww.mall.consumer.template.MemberRegisterMsgConsumer;
import com.ww.mall.consumer.template.MsgConsumerTemplate;
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
public class MallMemberConsumer {

    @RabbitListener(queues = {QueueConstant.MALL_MEMBER_REGISTER_QUEUE_NAME})
    public void memberRegisterMessage(Message message, Long memberId, Channel channel) throws IOException {
        log.info("收到mall_member服务发送新用户注册的消息：{}", memberId);
        MsgConsumerTemplate<Long> memberRegisterMsgConsumer = new MemberRegisterMsgConsumer();
        memberRegisterMsgConsumer.consumer(message, memberId, channel);
    }

}
