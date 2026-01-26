package com.ww.app.rabbitmq.template;

import com.rabbitmq.client.Channel;
import com.ww.app.rabbitmq.common.RabbitmqConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
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

        if (supportsBulkAcknowledgment() && deliveryTags.size() > 1 && areDeliveryTagsContinuous(deliveryTags)) {
            long maxTag = getMaxDeliveryTag(deliveryTags);
            channel.basicAck(maxTag, true);
            log.info("批量确认消息成功 [数量: {}] [最大标签: {}]", deliveryTags.size(), maxTag);
            return;
        }

        for (Long tag : deliveryTags) {
            channel.basicAck(tag, false);
        }
        log.info("逐条确认消息成功 [数量: {}]", deliveryTags.size());
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
        return false;
    }

    /**
     * 获取最大重试次数
     */
    protected int getMaxRetryCount() {
        return DEFAULT_MAX_RETRY_COUNT;
    }

    protected boolean areDeliveryTagsContinuous(List<Long> deliveryTags) {
        if (deliveryTags == null || deliveryTags.size() < 2) {
            return true;
        }
        long[] sorted = deliveryTags.stream().mapToLong(Long::longValue).sorted().toArray();
        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i] != sorted[i - 1] + 1) {
                return false;
            }
        }
        return true;
    }

    protected long getMaxDeliveryTag(List<Long> deliveryTags) {
        long max = Long.MIN_VALUE;
        for (Long tag : deliveryTags) {
            if (tag != null && tag > max) {
                max = tag;
            }
        }
        return max;
    }

    protected int getRetryCount(MessageProperties properties) {
        if (properties == null || properties.getHeaders() == null) {
            return 0;
        }
        Object value = properties.getHeaders().get(RabbitmqConstant.RETRY_COUNT_HEADER);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    protected int incrementRetryCount(MessageProperties properties) {
        int current = getRetryCount(properties);
        int next = current + 1;
        if (properties != null) {
            properties.setHeader(RabbitmqConstant.RETRY_COUNT_HEADER, next);
        }
        return next;
    }
}
