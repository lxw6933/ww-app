package com.ww.app.rabbitmq.template;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import com.rabbitmq.client.Channel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: ww
 * @create: 2023/7/22 22:32
 * @description: 专门用于批量入库等真正的批量操作场景，将多条消息作为一个整体进行处理【 批量操作消费者模板】
 **/
@Slf4j
public abstract class BatchOperationConsumerTemplate<T, R> extends AbstractMsgConsumerTemplate<T> {

    /**
     * 最小批处理阈值，低于此阈值不执行批处理
     */
    private static final int DEFAULT_MIN_BATCH_SIZE = 10;

    /**
     * 批量消费入口方法
     * 所有消息作为一个整体进行批量处理
     *
     * @param messages 原始消息列表
     * @param msgList  转换后的业务消息体列表
     * @param channel  RabbitMQ通道
     * @throws IOException 处理过程中可能的IO异常
     */
    public final void batchConsumer(List<Message> messages, List<T> msgList, Channel channel) throws IOException {
        if (CollUtil.isEmpty(messages) || CollUtil.isEmpty(msgList)) {
            log.warn("批量消息为空，消息数: {}, 业务消息数: {}", 
                     messages != null ? messages.size() : 0, 
                     msgList != null ? msgList.size() : 0);
            return;
        }

        // 设置批次ID用于日志跟踪
        String batchId = UUID.randomUUID(true).toString();

        int batchSize = messages.size();
        log.info("开始批量处理 [批次ID: {}] [批次大小: {}]", batchId, batchSize);
        try {
            // 对消息进行预处理和校验
            BatchProcessContext<T> processContext = preProcessBatch(messages, msgList);
            // 检查批处理上下文是否有效
            if (!processContext.isValid() || CollectionUtils.isEmpty(processContext.getValidMessages())) {
                log.info("批量消息预处理后无有效数据 [批次ID: {}]", batchId);
                // 确认所有消息，因为无需处理
                confirmAllMessages(channel, messages);
                return;
            }
            
            // 如果有效消息数量低于阈值，可以选择拒绝批处理
            if (processContext.getValidMessages().size() < getMinBatchSize()) {
                log.info("有效消息数量({})低于批处理阈值({}), 将逐条处理 [批次ID: {}]",
                        processContext.getValidMessages().size(), getMinBatchSize(), batchId);
                // 处理无效消息 - 直接确认
                confirmInvalidMessages(channel, processContext.getInvalidMessageTags());
                // 逐条处理少量消息
                processSingleMessages(channel, processContext);
                return;
            }
            // 执行批量业务处理
            BatchProcessResult<R> batchResult = processBatch(processContext.getValidMsgList());
            
            if (batchResult.isSuccess()) {
                // 批量处理成功，确认所有消息
                log.info("批量处理成功 [批次ID: {}] [处理数量: {}]", batchId, processContext.getValidMsgList().size());
                // 确认所有有效消息
                confirmMessages(channel, processContext.getValidMessageTags());
                // 确认所有无效消息
                confirmInvalidMessages(channel, processContext.getInvalidMessageTags());
                // 后置处理
                postProcessBatchSuccess(batchResult.getResult(), processContext);
            } else {
                // 批量处理失败，根据错误处理策略决定如何处理
                log.error("批量处理失败 [批次ID: {}] [错误: {}]", batchId, batchResult.getErrorMessage());
                // 根据失败处理策略处理消息
                handleBatchFailure(channel, processContext, batchResult);
            }
        } catch (Exception e) {
            log.error("批量处理过程中发生异常 [批次ID: {}]", batchId, e);
            // 处理异常情况
            handleProcessException(channel, messages, e);
        }
    }

    /**
     * 预处理批量消息
     * 将消息分为有效和无效两组
     */
    protected BatchProcessContext<T> preProcessBatch(List<Message> messages, List<T> msgList) {
        BatchProcessContext<T> context = new BatchProcessContext<>();
        context.setOriginalMessages(messages);
        context.setOriginalMsgList(msgList);
        
        List<T> validMsgList = new ArrayList<>();
        List<Long> validMessageTags = new ArrayList<>();
        List<Long> invalidMessageTags = new ArrayList<>();
        Map<Long, T> msgMap = new HashMap<>();
        Map<Long, Message> messageMap = new HashMap<>();
        
        for (int i = 0; i < messages.size(); i++) {
            if (i >= msgList.size()) {
                break;
            }
            
            Message message = messages.get(i);
            T msg = msgList.get(i);
            MessageProperties properties = message.getMessageProperties();
            long deliveryTag = properties.getDeliveryTag();
            
            // 检查消息是否有效
            if (isValidMessage(message, msg)) {
                validMsgList.add(msg);
                validMessageTags.add(deliveryTag);
                msgMap.put(deliveryTag, msg);
                messageMap.put(deliveryTag, message);
            } else {
                invalidMessageTags.add(deliveryTag);
            }
        }
        
        context.setValidMsgList(validMsgList);
        context.setValidMessageTags(validMessageTags);
        context.setInvalidMessageTags(invalidMessageTags);
        context.setMsgMap(msgMap);
        context.setMessageMap(messageMap);
        context.setValid(!validMsgList.isEmpty());
        return context;
    }

    /**
     * 逐条处理少量消息（低于阈值的情况）
     */
    protected void processSingleMessages(Channel channel, BatchProcessContext<T> context) throws IOException {
        Map<Long, T> messages = context.getValidMessages();
        if (CollUtil.isEmpty(messages)) {
            return;
        }
        
        List<Long> successTags = new ArrayList<>();
        Map<Long, String> failedTags = new HashMap<>();
        
        for (Map.Entry<Long, T> entry : messages.entrySet()) {
            long tag = entry.getKey();
            T msg = entry.getValue();
            
            try {
                boolean success = doProcess(msg);
                if (success) {
                    successTags.add(tag);
                } else {
                    failedTags.put(tag, "处理返回失败");
                }
            } catch (Exception e) {
                log.error("单条消息处理异常 [投递标签: {}]", tag, e);
                failedTags.put(tag, e.getMessage());
            }
        }
        
        // 确认成功的消息
        for (Long tag : successTags) {
            ackMessage(channel, tag);
        }
        
        // 处理失败的消息
        for (Map.Entry<Long, String> entry : failedTags.entrySet()) {
            long tag = entry.getKey();
            Message message = context.getMessageMap().get(tag);
            MessageProperties properties = message != null ? message.getMessageProperties() : null;
            int currentRetry = incrementRetryCount(properties);
            boolean shouldRequeue = shouldRequeueOnSingleFailure() && currentRetry <= getMaxRetryCount();
            nackMessage(channel, tag, shouldRequeue);
            if (!shouldRequeue) {
                handleDeadLetterMessage(tag, context.getMsgMap().get(tag), new RuntimeException(entry.getValue()));
            }
        }
    }

    /**
     * 确认所有消息
     */
    protected void confirmAllMessages(Channel channel, List<Message> messages) throws IOException {
        if (CollUtil.isEmpty(messages)) {
            return;
        }
        
        // 收集所有deliveryTag
        List<Long> tags = messages.stream()
                .map(msg -> msg.getMessageProperties().getDeliveryTag())
                .collect(Collectors.toList());
        
        confirmMessages(channel, tags);
    }
    
    /**
     * 确认消息列表
     */
    protected void confirmMessages(Channel channel, List<Long> deliveryTags) throws IOException {
        if (CollUtil.isEmpty(deliveryTags)) {
            return;
        }
        batchAckMessages(channel, deliveryTags);
    }

    /**
     * 确认无效消息（预处理阶段被过滤掉的消息）
     */
    protected void confirmInvalidMessages(Channel channel, List<Long> invalidTags) throws IOException {
        if (CollUtil.isEmpty(invalidTags)) {
            return;
        }
        
        // 默认直接确认无效消息
        confirmMessages(channel, invalidTags);
        log.info("确认无效消息 [数量: {}]", invalidTags.size());
    }

    /**
     * 处理批处理失败的情况
     */
    protected void handleBatchFailure(Channel channel, BatchProcessContext<T> context, BatchProcessResult<R> result) throws IOException {
        Exception error = result.getError() != null ? result.getError() : new RuntimeException(result.getErrorMessage());
        for (Long tag : context.getValidMessageTags()) {
            Message message = context.getMessageMap().get(tag);
            MessageProperties properties = message != null ? message.getMessageProperties() : null;
            int currentRetry = incrementRetryCount(properties);
            boolean shouldRequeue = shouldRetryOnBatchFailure(error) && currentRetry <= getMaxRetryCount();
            nackMessage(channel, tag, shouldRequeue);
            if (!shouldRequeue) {
                handleDeadLetterMessage(tag, context.getMsgMap().get(tag), error);
            }
        }
        log.info("批处理失败处理完成 [数量: {}]", context.getValidMessageTags().size());
        
        // 确认无效消息
        confirmInvalidMessages(channel, context.getInvalidMessageTags());
    }

    /**
     * 处理处理过程中发生异常的情况
     */
    protected void handleProcessException(Channel channel, List<Message> messages, Exception e) throws IOException {
        for (Message message : messages) {
            MessageProperties properties = message.getMessageProperties();
            long deliveryTag = properties.getDeliveryTag();
            int currentRetry = incrementRetryCount(properties);
            boolean shouldRequeue = shouldRetryOnException(e) && currentRetry <= getMaxRetryCount();
            nackMessage(channel, deliveryTag, shouldRequeue);
        }
        
        log.error("批处理过程异常，已拒绝所有消息 [数量: {}]", messages.size());
    }

    /**
     * 处理死信消息
     */
    protected void handleDeadLetterMessage(long deliveryTag, T message, Exception exception) {
        log.error("消息进入死信队列 [投递标签: {}] [异常: {}]", deliveryTag, exception.getMessage());
    }

    /**
     * 批处理成功后的后置处理
     */
    protected void postProcessBatchSuccess(R result, BatchProcessContext<T> context) {
        // 默认不执行任何后置处理
    }
    
    /**
     * 判断消息是否有效
     * 子类可以重写此方法进行自定义的消息有效性验证
     */
    protected boolean isValidMessage(Message message, T msg) {
        return msg != null;
    }
    
    /**
     * 获取最小批处理阈值
     */
    protected int getMinBatchSize() {
        return DEFAULT_MIN_BATCH_SIZE;
    }
    
    /**
     * 单条消息处理失败时是否应该重试
     */
    protected boolean shouldRequeueOnSingleFailure() {
        return true;
    }
    
    /**
     * 批量处理失败时是否应该重试
     */
    protected boolean shouldRetryOnBatchFailure(Exception e) {
        return true;
    }
    
    /**
     * 处理过程异常时是否应该重试
     */
    protected boolean shouldRetryOnException(Exception e) {
        return true;
    }

    @Override
    protected boolean supportsBulkAcknowledgment() {
        return true;
    }
    
    /**
     * 处理单条消息
     * 当消息数量低于阈值时使用
     */
    protected abstract boolean doProcess(T msg);
    
    /**
     * 批量处理消息
     * 核心批量业务处理逻辑，如批量入库等
     *
     * @param validMsgList 有效的业务消息列表
     * @return 批处理结果
     */
    protected abstract BatchProcessResult<R> processBatch(List<T> validMsgList);
    
    /**
     * 批量处理上下文
     */
    @Data
    protected static class BatchProcessContext<T> {
        // 原始消息列表
        private List<Message> originalMessages;
        // 原始业务消息列表
        private List<T> originalMsgList;
        // 有效业务消息列表
        private List<T> validMsgList;
        // 有效消息的deliveryTag列表
        private List<Long> validMessageTags;
        // 无效消息的deliveryTag列表
        private List<Long> invalidMessageTags;
        // deliveryTag到消息的映射
        private Map<Long, T> msgMap;
        // deliveryTag到原始Message的映射
        private Map<Long, Message> messageMap;
        // 上下文是否有效
        private boolean valid;
        
        public Map<Long, T> getValidMessages() {
            if (CollUtil.isEmpty(validMessageTags) || CollUtil.isEmpty(msgMap)) {
                return Collections.emptyMap();
            }
            
            return validMessageTags.stream()
                    .filter(msgMap::containsKey)
                    .collect(Collectors.toMap(Function.identity(), msgMap::get));
        }
    }
    
    /**
     * 批处理结果
     */
    @Data
    public static class BatchProcessResult<R> {
        // 处理结果
        private R result;
        // 是否成功
        private boolean success;
        // 错误信息
        private String errorMessage;
        // 错误异常
        private Exception error;
        
        public static <R> BatchProcessResult<R> success(R result) {
            BatchProcessResult<R> batchResult = new BatchProcessResult<>();
            batchResult.setResult(result);
            batchResult.setSuccess(true);
            return batchResult;
        }
        
        public static <R> BatchProcessResult<R> failure(String errorMessage) {
            BatchProcessResult<R> batchResult = new BatchProcessResult<>();
            batchResult.setSuccess(false);
            batchResult.setErrorMessage(errorMessage);
            return batchResult;
        }
        
        public static <R> BatchProcessResult<R> failure(String errorMessage, Exception error) {
            BatchProcessResult<R> batchResult = new BatchProcessResult<>();
            batchResult.setSuccess(false);
            batchResult.setErrorMessage(errorMessage);
            batchResult.setError(error);
            return batchResult;
        }
    }
} 
