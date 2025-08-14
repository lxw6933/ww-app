package com.ww.app.rabbitmq.template;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 消息消费模板抽象基类，提供所有消费者模板共用的基础功能
 * 
 * @param <T> 消息类型
 * @author ww
 */
@Slf4j
public abstract class AbstractMsgConsumerTemplate<T> {

    /**
     * 默认最大重试次数
     */
    protected static final int DEFAULT_MAX_RETRY_COUNT = 3;

    /**
     * 处理死信消息
     */
    protected void handleDeadLetterMessage(long deliveryTag, Exception exception) {
        log.error("消息进入死信队列 [投递标签: {}] [异常: {}]", deliveryTag, exception.getMessage());
    }

    /**
     * 处理死信消息(带消息体)
     */
    protected void handleDeadLetterMessage(long deliveryTag, T message, Exception exception) {
        log.error("消息进入死信队列 [投递标签: {}] [异常: {}]", deliveryTag, exception.getMessage());
    }

    /**
     * 消息确认
     */
    protected void ackMessage(Channel channel, long deliveryTag) throws IOException {
        channel.basicAck(deliveryTag, false);
    }

    /**
     * 消息拒绝
     */
    protected void nackMessage(Channel channel, long deliveryTag, boolean requeue) throws IOException {
        channel.basicNack(deliveryTag, false, requeue);
    }

    /**
     * 批量确认消息
     */
    protected void batchAckMessages(Channel channel, List<Long> deliveryTags) throws IOException {
        if (deliveryTags == null || deliveryTags.isEmpty()) {
            return;
        }

        // 是否支持批量确认
        if (supportsBulkAcknowledgment() && deliveryTags.size() > 1) {
            // 批量确认 - 确认到最大的deliveryTag
            long maxTag = Collections.max(deliveryTags);
            channel.basicAck(maxTag, true);
            log.info("批量确认消息成功 [数量: {}] [最大标签: {}]", deliveryTags.size(), maxTag);
        } else {
            // 逐条确认
            for (Long tag : deliveryTags) {
                channel.basicAck(tag, false);
            }
            log.info("逐条确认消息成功 [数量: {}]", deliveryTags.size());
        }
    }

    /**
     * 记录异常日志
     */
    protected void logException(String messageId, long deliveryTag, Exception e) {
        log.error("消息处理异常 [消息ID: {}] [投递标签: {}] [异常: {}]", 
                messageId, deliveryTag, e.getMessage(), e);
    }

    /**
     * 处理过程异常时是否应该重试
     */
    protected boolean shouldRetryOnException(Exception e) {
        return true;
    }

    /**
     * 是否支持批量确认
     */
    protected boolean supportsBulkAcknowledgment() {
        return true;
    }

    /**
     * 获取最大重试次数
     */
    protected int getMaxRetryCount() {
        return DEFAULT_MAX_RETRY_COUNT;
    }
} 