package com.ww.app.rabbitmq.template;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: ww
 * @create: 2023/7/22 22:32
 * @description: 消息消费模板，提供通用的消息处理、确认、重试等机制【单条消息消费模板，每次处理一条消息】
 **/
@Slf4j
public abstract class MsgConsumerTemplate<T> extends AbstractMsgConsumerTemplate<T> {
    
    /**
     * 当前重试次数记录
     */
    private final ThreadLocal<AtomicInteger> retryCount = ThreadLocal.withInitial(AtomicInteger::new);
    
    /**
     * 消息消费入口方法
     * 
     * @param message 原始消息
     * @param msg 转换后的业务消息体
     * @param channel RabbitMQ通道
     * @throws IOException 处理过程中可能的IO异常
     */
    public final void consumer(Message message, T msg, Channel channel) throws IOException {
        MessageProperties properties = message.getMessageProperties();

        long deliveryTag = properties.getDeliveryTag();
        String correlationId = Optional.ofNullable(properties.getCorrelationId()).orElse("未指定");
        
        log.info("开始消费消息 [消息ID: {}] [投递标签: {}] [消息体: {}]", correlationId, deliveryTag, msg);
        
        try {
            // 消费前置处理
            if (!preMsgConsumer(properties, channel)) {
                log.info("消息前置处理返回false，跳过此消息 [消息ID: {}]", correlationId);
                // 判断是否需要重新入队
                nackMessage(channel, deliveryTag, shouldRequeueOnPreConsumerFailure());
                return;
            }
            
            // 核心业务处理
            boolean result = doProcess(msg);
            
            if (result) {
                // 业务处理成功
                successMsgHandler(properties, channel);
                // 确认消息已消费
                ackMessage(channel, deliveryTag);
                log.info("消息处理成功并已确认 [消息ID: {}] [投递标签: {}]", correlationId, deliveryTag);
            } else {
                // 业务处理返回失败
                log.warn("业务处理返回失败标识 [消息ID: {}] [投递标签: {}]", correlationId, deliveryTag);
                handleBusinessFailure(properties, channel, deliveryTag);
            }
        } catch (Exception e) {
            // 异常消费处理
            exceptionMsgHandler(properties, channel, e);
            // 处理消息拒绝逻辑
            handleMessageException(channel, deliveryTag, e);
        } finally {
            // 清理ThreadLocal
            retryCount.remove();
        }
    }

    /**
     * 处理业务逻辑返回失败的情况
     */
    protected void handleBusinessFailure(MessageProperties properties, Channel channel, long deliveryTag) throws IOException {
        // 默认拒绝消息并重新入队
        nackMessage(channel, deliveryTag, shouldRequeueOnBusinessFailure());
        log.warn("业务处理失败，消息已拒绝 [消息ID: {}] [投递标签: {}] [重新入队: {}]", 
                properties.getCorrelationId(), deliveryTag, shouldRequeueOnBusinessFailure());
    }
    
    /**
     * 处理消息异常情况
     */
    protected void handleMessageException(Channel channel, long deliveryTag, Exception e) throws IOException {
        int currentRetry = retryCount.get().incrementAndGet();
        
        if (currentRetry <= getMaxRetryCount() && shouldRetryOnException(e)) {
            // 拒绝消息并重新入队尝试重试
            nackMessage(channel, deliveryTag, true);
            log.warn("消息处理异常，将进行第{}次重试", currentRetry);
        } else {
            // 超过最大重试次数或不应该重试的异常，拒绝消息且不重新入队
            nackMessage(channel, deliveryTag, false);
            log.error("消息处理最终失败，已拒绝且不再重试");
            
            // 处理死信消息
            handleDeadLetterMessage(deliveryTag, e);
        }
    }

    /**
     * 消息处理成功后的处理
     */
    protected void successMsgHandler(MessageProperties properties, Channel channel) {
        log.info("消息处理成功 [消息ID: {}] [投递标签: {}] [处理时间: {}]", 
                properties.getCorrelationId(), properties.getDeliveryTag(), LocalDateTime.now());
    }

    /**
     * 消息消费前的处理，返回false将跳过此消息
     */
    protected boolean preMsgConsumer(MessageProperties properties, Channel channel) {
        return true;
    }

    /**
     * 消息异常处理
     */
    protected void exceptionMsgHandler(MessageProperties properties, Channel channel, Exception e) {
        logException(properties.getCorrelationId(), properties.getDeliveryTag(), e);
    }
    
    /**
     * 业务处理失败时是否应该重新入队
     * 默认重新入队尝试重试
     */
    protected boolean shouldRequeueOnBusinessFailure() {
        return true;
    }
    
    /**
     * 前置处理失败时是否应该重新入队
     * 默认不重新入队
     */
    protected boolean shouldRequeueOnPreConsumerFailure() {
        return false;
    }
    
    /**
     * 核心业务处理方法，子类必须实现
     *
     * @param msg 消息内容
     * @return 处理是否成功
     */
    protected abstract boolean doProcess(T msg);
}
