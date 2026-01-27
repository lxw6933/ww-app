package com.ww.app.consumer.server.dead;

import com.rabbitmq.client.Channel;
import com.ww.app.rabbitmq.common.RabbitmqConstant;
import com.ww.app.rabbitmq.queue.QueueConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 全局死信队列消费者
 */
@Slf4j
@Component
public class DeadLetterConsumer {

    private final DeadLetterHandler handler;

    public DeadLetterConsumer(DeadLetterHandler handler) {
        this.handler = handler;
    }

    @RabbitListener(queues = {QueueConstant.CONSUME_FAIL_QUEUE}, containerFactory = "msgConsumerContainerFactory")
    public void onDeadLetter(Message message, Channel channel) throws IOException {
        MessageProperties props = message.getMessageProperties();
        log.info("MQ死信消费[queue={}] correlationId={} exchange={} routingKey={} retryCount={} messageId={}",
                QueueConstant.CONSUME_FAIL_QUEUE,
                props.getCorrelationId(),
                props.getHeader(RabbitmqConstant.EXCHANGE_HEADER),
                props.getHeader(RabbitmqConstant.ROUTING_KEY_HEADER),
                props.getHeader(RabbitmqConstant.RETRY_COUNT_HEADER),
                props.getMessageId());
        handler.onDeadLetter(message);
        channel.basicAck(props.getDeliveryTag(), false);
    }
}
