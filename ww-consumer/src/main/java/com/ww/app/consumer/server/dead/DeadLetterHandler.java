package com.ww.app.consumer.server.dead;

import org.springframework.amqp.core.Message;

/**
 * 死信消息处理钩子
 */
public interface DeadLetterHandler {

    void onDeadLetter(Message message);
}
