package com.ww.app.rabbitmq.template;

import cn.hutool.core.lang.UUID;
import com.rabbitmq.client.Channel;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.thread.ThreadMdcUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: ww
 * @create: 2023/7/22 22:32
 * @description: 消息消费模板，提供通用的消息处理、确认、重试等机制
 **/
@Slf4j
public abstract class MsgConsumerTemplate<T> {

    /**
     * 默认最大重试次数
     */
    private static final int DEFAULT_MAX_RETRY_COUNT = 3;
    
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
        String traceId = properties.getHeader(Constant.TRACE_ID);
        // 如果没有traceId则自动生成一个
        ThreadMdcUtil.setTraceId(StringUtils.hasText(traceId) ? traceId : UUID.randomUUID(true).toString());
        
        long deliveryTag = properties.getDeliveryTag();
        String correlationId = Optional.ofNullable(properties.getCorrelationId()).orElse("未指定");
        
        log.info("开始消费消息 [消息ID: {}] [投递标签: {}] [消息体: {}]", correlationId, deliveryTag, msg);
        
        try {
            // 消费前置处理
            if (!preMsgConsumer(properties, channel)) {
                log.info("消息前置处理返回false，跳过此消息 [消息ID: {}]", correlationId);
                // 判断是否需要重新入队
                handleMessageRejection(channel, deliveryTag, shouldRequeueOnPreConsumerFailure());
                return;
            }
            
            // 核心业务处理
            boolean result = serverHandler(msg);
            
            if (result) {
                // 业务处理成功
                successMsgHandler(properties, channel);
                // 确认消息已消费
                channel.basicAck(deliveryTag, false);
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
            MDC.remove(Constant.TRACE_ID);
        }
    }

    /**
     * 处理业务逻辑返回失败的情况
     */
    protected void handleBusinessFailure(MessageProperties properties, Channel channel, long deliveryTag) throws IOException {
        // 默认拒绝消息并重新入队
        channel.basicNack(deliveryTag, false, shouldRequeueOnBusinessFailure());
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
            channel.basicNack(deliveryTag, false, true);
            log.warn("消息处理异常，将进行第{}次重试", currentRetry);
        } else {
            // 超过最大重试次数或不应该重试的异常，拒绝消息且不重新入队
            channel.basicNack(deliveryTag, false, false);
            log.error("消息处理最终失败，已拒绝且不再重试");
            
            // 处理死信消息
            handleDeadLetterMessage(deliveryTag, e);
        }
    }
    
    /**
     * 处理消息拒绝
     */
    protected void handleMessageRejection(Channel channel, long deliveryTag, boolean requeue) throws IOException {
        channel.basicNack(deliveryTag, false, requeue);
    }
    
    /**
     * 处理进入死信队列的消息
     */
    protected void handleDeadLetterMessage(long deliveryTag, Exception exception) {
        // 默认只记录日志，子类可以重写此方法实现死信消息的处理逻辑
        log.error("消息进入死信队列 [投递标签: {}] [异常: {}]", deliveryTag, exception.getMessage());
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
     * 服务核心业务逻辑处理
     *
     * @param msg 消息
     * @return 业务是否成功
     */
    public abstract boolean serverHandler(T msg);

    /**
     * 异常消息处理
     */
    protected void exceptionMsgHandler(MessageProperties properties, Channel channel, Exception e) {
        log.error("消息处理异常 [消息ID: {}] [投递标签: {}] [异常: {}]", 
                properties.getCorrelationId(), properties.getDeliveryTag(), e.getMessage(), e);
    }
    
    /**
     * 获取最大重试次数
     */
    protected int getMaxRetryCount() {
        return DEFAULT_MAX_RETRY_COUNT;
    }
    
    /**
     * 判断是否应该在异常情况下重试
     * 默认所有异常都重试，子类可以根据具体异常类型判断是否需要重试
     */
    protected boolean shouldRetryOnException(Exception e) {
        return true;
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
}
