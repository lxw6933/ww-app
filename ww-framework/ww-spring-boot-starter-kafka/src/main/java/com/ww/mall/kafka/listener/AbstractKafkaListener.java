package com.ww.mall.kafka.listener;

import com.ww.mall.kafka.exception.KafkaException;
import com.ww.mall.kafka.template.KafkaOperations;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.support.Acknowledgment;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Kafka监听器抽象类
 * 提供通用的消息处理、异常处理和重试机制
 */
public abstract class AbstractKafkaListener {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    @Resource
    private KafkaOperations kafkaOperations;
    
    // 消息跟踪头
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String RETRY_COUNT_HEADER = "X-Retry-Count";
    private static final String ORIGINAL_TOPIC_HEADER = "X-Original-Topic";
    private static final String ERROR_MESSAGE_HEADER = "X-Error-Message";
    
    // 本地重试计数器
    private final ThreadLocal<AtomicInteger> retryCounter = ThreadLocal.withInitial(AtomicInteger::new);
    
    // 死信队列后缀
    private static final String DLQ_SUFFIX = ".dlq";
    
    /**
     * 处理单条消息
     */
    protected void processRecord(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String traceId = extractTraceId(record);
        try {
            // 设置MDC上下文便于日志追踪
            MDC.put("traceId", traceId);
            
            if (isDebugEnabled()) {
                log.debug("接收到消息: topic={}, partition={}, offset={}, key={}, value={}",
                        record.topic(), record.partition(), record.offset(), record.key(), record.value());
            }
            
            // 前置处理
            beforeProcess(record);
            
            // 调用子类实现的消息处理方法
            processMessage(record.key(), record.value(), record);
            
            // 后置处理
            afterProcess(record);
            
            // 确认消息已处理
            ack.acknowledge();
            
        } catch (Exception e) {
            handleException(record, e, ack);
        } finally {
            MDC.remove("traceId");
            retryCounter.remove();
        }
    }
    
    /**
     * 处理批量消息
     */
    protected void processBatchRecords(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        try {
            if (isDebugEnabled()) {
                log.debug("接收到批量消息: size={}", records.size());
            }
            // 前置批量处理
            beforeBatchProcess(records);
            // 调用子类实现的批量消息处理方法
            processMessages(records);
            // 后置批量处理
            afterBatchProcess(records);
            // 确认所有消息已处理
            ack.acknowledge();
        } catch (Exception e) {
            handleBatchException(records, e, ack);
        }
    }
    
    /**
     * 提取消息头信息
     */
    protected Map<String, String> extractHeaders(ConsumerRecord<String, String> record) {
        Map<String, String> headers = new HashMap<>();
        record.headers().forEach(header -> 
            headers.put(header.key(), new String(header.value()))
        );
        return headers;
    }
    
    /**
     * 提取跟踪ID
     */
    protected String extractTraceId(ConsumerRecord<String, String> record) {
        Map<String, String> headers = extractHeaders(record);
        String traceId = headers.get(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        return traceId;
    }
    
    /**
     * 获取重试次数
     */
    protected int getRetryCount(ConsumerRecord<String, String> record) {
        Map<String, String> headers = extractHeaders(record);
        try {
            String retryCountStr = headers.get(RETRY_COUNT_HEADER);
            return retryCountStr != null ? Integer.parseInt(retryCountStr) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * 从批量消息中提取消息内容列表
     */
    protected List<String> extractValues(List<ConsumerRecord<String, String>> records) {
        return records.stream()
                .map(ConsumerRecord::value)
                .collect(Collectors.toList());
    }
    
    /**
     * 处理消息处理异常
     */
    protected void handleException(ConsumerRecord<String, String> record, Exception e, Acknowledgment ack) {
        String traceId = extractTraceId(record);
        
        log.error("处理消息失败 [traceId={}]: topic={}, partition={}, offset={}, key={}, error={}",
                traceId, record.topic(), record.partition(), record.offset(),
                record.key(), e.getMessage(), e);
        
        // 获取当前重试次数
        int currentRetries = retryCounter.get().incrementAndGet();
        int maxRetries = getMaxRetries();
        
        // 根据异常类型和重试次数决定是否重试
        if (shouldRetry(e) && currentRetries <= maxRetries) {
            // 执行重试逻辑
            onRetry(record, e);
            // 不确认消息，触发重试
            log.warn("消息将被重试 [traceId={}]: 当前重试次数={}, 最大重试次数={}", 
                    traceId, currentRetries, maxRetries);
        } else {
            // 执行错误处理逻辑
            onError(record, e);
            
            // 发送到死信队列
            if (isDeadLetterQueueEnabled()) {
                sendToDeadLetterQueue(record, e);
            }
            
            // 确认消息以避免无限重试
            log.warn("消息处理失败但不会重试，已确认 [traceId={}]: key={}", traceId, record.key());
            ack.acknowledge();
        }
    }
    
    /**
     * 处理批量消息处理异常
     */
    protected void handleBatchException(List<ConsumerRecord<String, String>> records, Exception e, Acknowledgment ack) {
        log.error("处理批量消息失败: size={}, error={}",
                records.size(), e.getMessage(), e);
        
        // 根据异常类型决定是否重试
        if (shouldRetry(e)) {
            // 执行批量重试逻辑
            onBatchRetry(records, e);
            // 不确认消息，触发重试
            log.warn("批量消息将被重试: size={}", records.size());
        } else {
            // 执行批量错误处理逻辑
            onBatchError(records, e);
            // 确认消息以避免无限重试
            log.warn("批量消息处理失败但不会重试，已确认: size={}", records.size());
            ack.acknowledge();
        }
    }
    
    /**
     * 判断是否应该重试
     */
    protected boolean shouldRetry(Exception e) {
        // 默认规则：系统异常重试，业务异常不重试
        return !(e instanceof KafkaException);
    }
    
    /**
     * 是否启用调试日志
     */
    protected boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }
    
    /**
     * 消息处理前的钩子方法
     */
    protected void beforeProcess(ConsumerRecord<String, String> record) throws Exception {
        // 默认空实现，子类可覆盖
    }
    
    /**
     * 消息处理后的钩子方法
     */
    protected void afterProcess(ConsumerRecord<String, String> record) throws Exception {
        // 默认空实现，子类可覆盖
    }
    
    /**
     * 批量消息处理前的钩子方法
     */
    protected void beforeBatchProcess(List<ConsumerRecord<String, String>> records) throws Exception {
        // 默认空实现，子类可覆盖
    }
    
    /**
     * 批量消息处理后的钩子方法
     */
    protected void afterBatchProcess(List<ConsumerRecord<String, String>> records) throws Exception {
        // 默认空实现，子类可覆盖
    }
    
    /**
     * 消息重试时的钩子方法
     */
    protected void onRetry(ConsumerRecord<String, String> record, Exception e) {
        // 默认空实现，子类可覆盖
    }
    
    /**
     * 消息错误处理的钩子方法
     */
    protected void onError(ConsumerRecord<String, String> record, Exception e) {
        // 默认空实现，子类可覆盖
    }
    
    /**
     * 批量消息重试时的钩子方法
     */
    protected void onBatchRetry(List<ConsumerRecord<String, String>> records, Exception e) {
        // 默认空实现，子类可覆盖
    }
    
    /**
     * 批量消息错误处理的钩子方法
     */
    protected void onBatchError(List<ConsumerRecord<String, String>> records, Exception e) {
        // 默认空实现，子类可覆盖
    }
    
    /**
     * 发送消息到死信队列
     */
    protected void sendToDeadLetterQueue(ConsumerRecord<String, String> record, Exception e) {
        if (kafkaOperations == null) {
            log.warn("无法发送到死信队列，KafkaOperations未注入");
            return;
        }
        
        try {
            String dlqTopic = record.topic() + DLQ_SUFFIX;
            String traceId = extractTraceId(record);
            
            // 构建包含错误信息的消息头
            Map<String, String> headers = extractHeaders(record);
            headers.put(ORIGINAL_TOPIC_HEADER, record.topic());
            headers.put(ERROR_MESSAGE_HEADER, e.getMessage());
            headers.put(RETRY_COUNT_HEADER, String.valueOf(retryCounter.get().get()));
            headers.put(TRACE_ID_HEADER, traceId);
            
            // 发送到死信队列
            kafkaOperations.send(dlqTopic, record.key(), record.value(), headers)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("发送到死信队列失败 [traceId={}]: {}", traceId, ex.getMessage(), ex);
                    } else {
                        log.info("消息已发送到死信队列 [traceId={}]: topic={}", traceId, dlqTopic);
                    }
                });
        } catch (Exception ex) {
            log.error("发送到死信队列时出错: {}", ex.getMessage(), ex);
        }
    }
    
    /**
     * 是否启用死信队列
     */
    protected boolean isDeadLetterQueueEnabled() {
        return true; // 默认启用，子类可覆盖
    }
    
    /**
     * 获取最大重试次数
     */
    protected int getMaxRetries() {
        return 3; // 默认重试3次，子类可覆盖
    }
    
    /**
     * 消息处理抽象方法，由子类实现具体业务逻辑
     */
    protected abstract void processMessage(String key, String value, ConsumerRecord<String, String> record) throws Exception;
    
    /**
     * 批量消息处理，默认实现为单条处理，子类可覆盖提供批量处理逻辑
     */
    protected void processMessages(List<ConsumerRecord<String, String>> records) throws Exception {
        for (ConsumerRecord<String, String> record : records) {
            processMessage(record.key(), record.value(), record);
        }
    }
} 