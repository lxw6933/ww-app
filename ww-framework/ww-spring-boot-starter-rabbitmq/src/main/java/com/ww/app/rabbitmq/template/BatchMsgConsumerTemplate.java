package com.ww.app.rabbitmq.template;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import com.rabbitmq.client.Channel;
import com.ww.app.common.thread.ThreadMdcUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * @author: ww
 * @create: 2023/7/22 22:32
 * @description: 并行批量消息消费模板，适用于批量获取消息但需要并行处理的场景【支持并行处理、部分失败处理和批量确认】
 **/
@Slf4j
public abstract class BatchMsgConsumerTemplate<T> extends AbstractMsgConsumerTemplate<T> {

    /**
     * 批量消费入口方法
     *
     * @param messages 原始消息列表
     * @param msgList  转换后的业务消息体列表
     * @param channel  RabbitMQ通道
     * @throws IOException 处理过程中可能的IO异常
     */
    public final void batchConsumer(List<Message> messages, List<T> msgList, Channel channel) throws IOException {
        if (CollUtil.isEmpty(messages) || CollUtil.isEmpty(msgList) || messages.size() != msgList.size()) {
            log.warn("批量消息为空或消息数量不匹配，消息数: {}, 业务消息数: {}", 
                    messages != null ? messages.size() : 0, 
                    msgList != null ? msgList.size() : 0);
            return;
        }

        // 设置批次ID用于日志跟踪
        String batchId = UUID.randomUUID(true).toString();
        ThreadMdcUtil.setTraceId(batchId);
        
        int batchSize = messages.size();
        log.info("开始批量处理消息 [批次ID: {}] [批次大小: {}]", batchId, batchSize);
        
        try {
            // 对每条消息进行前置检查
            List<BatchMessageContext<T>> validMessages = preProcessMessages(messages, msgList, channel);
            
            if (validMessages.isEmpty()) {
                log.info("批量消息前置处理后无有效消息 [批次ID: {}]", batchId);
                return;
            }
            
            // 处理所有有效消息
            Map<Long, Boolean> processResults = processMessages(validMessages);
            
            // 根据处理结果分组
            List<Long> successTags = new ArrayList<>();
            Map<Long, Exception> failedTags = new HashMap<>();
            
            validMessages.forEach(context -> {
                long deliveryTag = context.getMessage().getMessageProperties().getDeliveryTag();
                Boolean success = processResults.get(deliveryTag);
                
                if (Boolean.TRUE.equals(success)) {
                    successTags.add(deliveryTag);
                } else {
                    failedTags.put(deliveryTag, new RuntimeException("业务处理失败"));
                }
            });
            
            // 处理成功的消息
            handleSuccessMessages(channel, successTags);
            
            // 处理失败的消息
            handleFailedMessages(channel, failedTags);
            
        } catch (Exception e) {
            log.error("批量消息处理过程中发生异常 [批次ID: {}]", batchId, e);
            // 批处理整体失败，根据策略决定是全部拒绝还是进行部分确认
            handleBatchException(channel, messages, e);
        } finally {
            cleanupContext();
        }
    }

    /**
     * 消息前置处理
     */
    protected List<BatchMessageContext<T>> preProcessMessages(List<Message> messages, List<T> msgList, Channel channel) {
        List<BatchMessageContext<T>> validMessages = new ArrayList<>();
        
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            T msg = msgList.get(i);
            MessageProperties properties = message.getMessageProperties();
            long deliveryTag = properties.getDeliveryTag();
            
            try {
                // 对每条消息进行前置检查
                if (shouldProcessMessage(message, msg)) {
                    validMessages.add(new BatchMessageContext<>(message, msg, deliveryTag));
                } else {
                    // 消息不需要处理，直接确认
                    log.info("消息不需要处理，直接确认 [投递标签: {}]", deliveryTag);
                    ackMessage(channel, deliveryTag);
                }
            } catch (Exception e) {
                log.warn("消息前置处理异常 [投递标签: {}]", deliveryTag, e);
                try {
                    // 拒绝此消息但不影响其他消息
                    nackMessage(channel, deliveryTag, shouldRequeueOnPreProcessingFailure());
                } catch (IOException ioe) {
                    log.error("消息拒绝失败 [投递标签: {}]", deliveryTag, ioe);
                }
            }
        }
        
        return validMessages;
    }

    /**
     * 批量处理消息
     * 默认使用CompletableFuture并行处理
     */
    protected Map<Long, Boolean> processMessages(List<BatchMessageContext<T>> messages) {
        Map<Long, Boolean> results = new ConcurrentHashMap<>(messages.size());
        
        // 是否使用并行处理
        if (useParallelProcessing() && messages.size() > 1) {
            // 获取并行执行器
            Executor executor = getParallelExecutor();
            // 等待所有任务完成
            CompletableFuture.allOf(messages.stream()
                     .map(context -> CompletableFuture.runAsync(() -> {
                         try {
                             long deliveryTag = context.getDeliveryTag();
                             // 执行业务处理
                             boolean success = doProcess(context.getMsg());
                             results.put(deliveryTag, success);
                         } catch (Exception e) {
                             log.error("消息处理异常 [投递标签: {}]", context.getDeliveryTag(), e);
                             results.put(context.getDeliveryTag(), false);
                         }
                     }, executor)).toArray(CompletableFuture[]::new)).join();
        } else {
            // 串行处理
            for (BatchMessageContext<T> context : messages) {
                try {
                    long deliveryTag = context.getDeliveryTag();
                    boolean success = doProcess(context.getMsg());
                    results.put(deliveryTag, success);
                } catch (Exception e) {
                    log.error("消息处理异常 [投递标签: {}]", context.getDeliveryTag(), e);
                    results.put(context.getDeliveryTag(), false);
                }
            }
        }
        
        return results;
    }
    
    /**
     * 处理成功的消息
     */
    protected void handleSuccessMessages(Channel channel, List<Long> deliveryTags) throws IOException {
        if (CollUtil.isEmpty(deliveryTags)) {
            return;
        }
        
        batchAckMessages(channel, deliveryTags);
    }
    
    /**
     * 处理失败的消息
     */
    protected void handleFailedMessages(Channel channel, Map<Long, Exception> failedMessages) throws IOException {
        if (CollUtil.isEmpty(failedMessages)) {
            return;
        }
        
        for (Map.Entry<Long, Exception> entry : failedMessages.entrySet()) {
            long tag = entry.getKey();
            Exception e = entry.getValue();
            
            // 判断是否应该重试
            boolean shouldRequeue = shouldRetryOnFailure(e);
            nackMessage(channel, tag, shouldRequeue);
            
            if (shouldRequeue) {
                log.warn("消息处理失败，将重新入队 [投递标签: {}]", tag);
            } else {
                log.error("消息处理失败，不再重试 [投递标签: {}]", tag);
                // 处理死信消息
                handleDeadLetterMessage(tag, e);
            }
        }
    }
    
    /**
     * 处理批量异常情况
     */
    protected void handleBatchException(Channel channel, List<Message> messages, Exception e) throws IOException {
        // 默认实现：拒绝所有消息并重新入队
        boolean shouldRequeue = shouldRetryOnBatchFailure(e);
        
        for (Message message : messages) {
            long deliveryTag = message.getMessageProperties().getDeliveryTag();
            nackMessage(channel, deliveryTag, shouldRequeue);
        }
        
        log.error("批量处理异常，已拒绝所有消息 [数量: {}] [重新入队: {}]", 
                messages.size(), shouldRequeue);
    }
    
    /**
     * 判断消息是否应该被处理
     * 默认所有消息都处理
     */
    protected boolean shouldProcessMessage(Message message, T msg) {
        return true;
    }
    
    /**
     * 是否使用并行处理
     * 默认使用
     */
    protected boolean useParallelProcessing() {
        return true;
    }
    
    /**
     * 获取并行执行器
     * 子类可以重写提供自定义的执行器
     */
    protected Executor getParallelExecutor() {
        return ForkJoinPool.commonPool();
    }
    
    /**
     * 单条消息处理失败时是否应该重试
     */
    protected boolean shouldRetryOnFailure(Exception e) {
        return true;
    }
    
    /**
     * 批量消息处理异常时是否应该重试
     */
    protected boolean shouldRetryOnBatchFailure(Exception e) {
        return true;
    }
    
    /**
     * 前置处理失败时是否应该重试
     */
    protected boolean shouldRequeueOnPreProcessingFailure() {
        return false;
    }
    
    /**
     * 处理单条消息的业务逻辑
     * 子类必须实现此方法
     *
     * @param msg 消息内容
     * @return 处理结果
     */
    protected abstract boolean doProcess(T msg);
    
    /**
     * 批量消息上下文
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    protected static class BatchMessageContext<T> {
        private Message message;
        private T msg;
        private long deliveryTag;
    }
} 