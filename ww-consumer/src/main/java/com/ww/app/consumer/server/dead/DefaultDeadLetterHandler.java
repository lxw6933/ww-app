package com.ww.app.consumer.server.dead;

import org.springframework.amqp.core.Message;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 默认死信处理器（空实现）
 */
@Component
@ConditionalOnMissingBean(DeadLetterHandler.class)
public class DefaultDeadLetterHandler implements DeadLetterHandler {
    @Override
    public void onDeadLetter(Message message) {
        // no-op
    }
}
