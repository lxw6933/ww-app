package com.ww.mall.kafka.template;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.StopWatch;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

/**
 * 自定义Kafka模板，提供增强功能
 */
public class KafkaOperations {
    private static final Logger log = LoggerFactory.getLogger(KafkaOperations.class);
    
    private final KafkaTemplate<String, String> stringTemplate;
    private final KafkaTemplate<String, Object> jsonTemplate;
    
    // 性能监控计数器
    private final AtomicLong totalMessagesSent = new AtomicLong(0);
    private final AtomicLong messagesSentSuccess = new AtomicLong(0);
    private final AtomicLong messagesSentFailed = new AtomicLong(0);
    
    // 消息跟踪头名称
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TIMESTAMP_HEADER = "X-Timestamp";
    
    /**
     * 构造方法
     */
    public KafkaOperations(KafkaTemplate<String, String> stringTemplate,
                          KafkaTemplate<String, Object> jsonTemplate) {
        this.stringTemplate = stringTemplate;
        this.jsonTemplate = jsonTemplate;
    }
    
    /**
     * 发送字符串消息
     */
    public CompletableFuture<SendResult<String, String>> send(String topic, String message) {
        ListenableFuture<SendResult<String, String>> future = stringTemplate.send(topic, message);
        return toCompletableFuture(future);
    }
    
    /**
     * 发送字符串消息（带Key）
     */
    public CompletableFuture<SendResult<String, String>> send(String topic, String key, String message) {
        ListenableFuture<SendResult<String, String>> future = stringTemplate.send(topic, key, message);
        return toCompletableFuture(future);
    }
    
    /**
     * 发送带有头信息的消息
     */
    public CompletableFuture<SendResult<String, String>> send(String topic, String key, String message, Map<String, String> headers) {
        StopWatch watch = new StopWatch();
        watch.start();
        
        totalMessagesSent.incrementAndGet();
        
        List<Header> headerList = new ArrayList<>();
        if (headers != null && !headers.isEmpty()) {
            headers.forEach((name, value) -> 
                headerList.add(new RecordHeader(name, value.getBytes(StandardCharsets.UTF_8)))
            );
        }
        
        // 添加跟踪信息
        String traceId = headers != null && headers.containsKey(TRACE_ID_HEADER) ? 
                headers.get(TRACE_ID_HEADER) : generateTraceId();
        headerList.add(new RecordHeader(TRACE_ID_HEADER, traceId.getBytes(StandardCharsets.UTF_8)));
        headerList.add(new RecordHeader(TIMESTAMP_HEADER, 
                String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8)));
        
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, null, key, message, headerList);
        ListenableFuture<SendResult<String, String>> future = stringTemplate.send(record);
        
        CompletableFuture<SendResult<String, String>> completableFuture = toCompletableFuture(future);
        completableFuture.whenComplete((result, ex) -> {
            watch.stop();
            long duration = watch.getTotalTimeMillis();
            
            if (ex == null) {
                messagesSentSuccess.incrementAndGet();
                if (log.isDebugEnabled()) {
                    log.debug("消息发送成功 [traceId={}] topic={}, partition={}, offset={}, 耗时={}ms", 
                            traceId, topic, result.getRecordMetadata().partition(), 
                            result.getRecordMetadata().offset(), duration);
                }
            } else {
                messagesSentFailed.incrementAndGet();
                log.error("消息发送失败 [traceId={}] topic={}, key={}, 耗时={}ms, 错误={}", 
                        traceId, topic, key, duration, ex.getMessage());
            }
        });
        
        return completableFuture;
    }
    
    /**
     * 发送消息到指定分区
     */
    public CompletableFuture<SendResult<String, String>> send(String topic, Integer partition, String key, String message) {
        ListenableFuture<SendResult<String, String>> future = stringTemplate.send(topic, partition, key, message);
        return toCompletableFuture(future);
    }
    
    /**
     * 发送消息并指定回调处理
     */
    public void sendWithCallback(String topic, String message, BiConsumer<SendResult<String, String>, Throwable> callback) {
        CompletableFuture<SendResult<String, String>> future = send(topic, message);
        future.whenComplete(callback);
    }
    
    /**
     * 发送JSON对象消息
     */
    public CompletableFuture<SendResult<String, Object>> sendJson(String topic, Object object) {
        ListenableFuture<SendResult<String, Object>> future = jsonTemplate.send(topic, object);
        return toCompletableFutureJson(future);
    }
    
    /**
     * 发送JSON对象消息（带Key）
     */
    public CompletableFuture<SendResult<String, Object>> sendJson(String topic, String key, Object object) {
        ListenableFuture<SendResult<String, Object>> future = jsonTemplate.send(topic, key, object);
        return toCompletableFutureJson(future);
    }
    
    /**
     * 发送JSON对象到指定分区
     */
    public CompletableFuture<SendResult<String, Object>> sendJson(String topic, Integer partition, String key, Object object) {
        ListenableFuture<SendResult<String, Object>> future = jsonTemplate.send(topic, partition, key, object);
        return toCompletableFutureJson(future);
    }
    
    /**
     * 同步发送字符串消息
     */
    public SendResult<String, String> sendSync(String topic, String message, long timeout) 
            throws InterruptedException, ExecutionException, TimeoutException {
        return stringTemplate.send(topic, message).get(timeout, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 同步发送字符串消息（带Key）
     */
    public SendResult<String, String> sendSync(String topic, String key, String message, long timeout) 
            throws InterruptedException, ExecutionException, TimeoutException {
        return stringTemplate.send(topic, key, message).get(timeout, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 同步发送JSON对象消息
     */
    public SendResult<String, Object> sendJsonSync(String topic, Object object, long timeout) 
            throws InterruptedException, ExecutionException, TimeoutException {
        return jsonTemplate.send(topic, object).get(timeout, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 同步发送JSON对象消息（带Key）
     */
    public SendResult<String, Object> sendJsonSync(String topic, String key, Object object, long timeout) 
            throws InterruptedException, ExecutionException, TimeoutException {
        return jsonTemplate.send(topic, key, object).get(timeout, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 批量发送消息
     */
    public List<CompletableFuture<SendResult<String, String>>> sendBatch(String topic, List<String> messages) {
        List<CompletableFuture<SendResult<String, String>>> futures = new ArrayList<>(messages.size());
        for (String message : messages) {
            futures.add(send(topic, message));
        }
        return futures;
    }
    
    /**
     * 批量发送消息（带Key）
     */
    public List<CompletableFuture<SendResult<String, String>>> sendBatch(String topic, String key, List<String> messages) {
        List<CompletableFuture<SendResult<String, String>>> futures = new ArrayList<>(messages.size());
        for (String message : messages) {
            futures.add(send(topic, key, message));
        }
        return futures;
    }
    
    /**
     * 将结果处理为回调函数的异步发送方式
     */
    public void sendAsync(String topic, String message, 
                         BiConsumer<SendResult<String, String>, Throwable> callback) {
        CompletableFuture<SendResult<String, String>> future = send(topic, message);
        future.whenComplete(callback);
    }
    
    /**
     * 获取原始String KafkaTemplate
     */
    public KafkaTemplate<String, String> getStringTemplate() {
        return stringTemplate;
    }
    
    /**
     * 获取原始JSON KafkaTemplate
     */
    public KafkaTemplate<String, Object> getJsonTemplate() {
        return jsonTemplate;
    }
    
    /**
     * 获取当前事务状态
     */
    public boolean isTransactional() {
        return stringTemplate.isTransactional();
    }
    
    /**
     * 在事务中执行操作
     */
    public <T> T executeInTransaction(KafkaTransactionCallback<T> callback) {
        return stringTemplate.executeInTransaction(operations -> callback.doInTransaction(this));
    }
    
    /**
     * 事务回调接口
     */
    @FunctionalInterface
    public interface KafkaTransactionCallback<T> {
        T doInTransaction(KafkaOperations operations);
    }
    
    /**
     * 转换ListenableFuture为CompletableFuture (String类型)
     */
    private CompletableFuture<SendResult<String, String>> toCompletableFuture(
            ListenableFuture<SendResult<String, String>> listenableFuture) {
        CompletableFuture<SendResult<String, String>> completableFuture = new CompletableFuture<>();
        
        listenableFuture.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
            @Override
            public void onFailure(Throwable ex) {
                completableFuture.completeExceptionally(ex);
            }
            
            @Override
            public void onSuccess(SendResult<String, String> result) {
                completableFuture.complete(result);
            }
        });
        
        return completableFuture;
    }
    
    /**
     * 转换ListenableFuture为CompletableFuture (JSON类型)
     */
    private CompletableFuture<SendResult<String, Object>> toCompletableFutureJson(
            ListenableFuture<SendResult<String, Object>> listenableFuture) {
        CompletableFuture<SendResult<String, Object>> completableFuture = new CompletableFuture<>();
        
        listenableFuture.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
            @Override
            public void onFailure(Throwable ex) {
                completableFuture.completeExceptionally(ex);
            }
            
            @Override
            public void onSuccess(SendResult<String, Object> result) {
                completableFuture.complete(result);
            }
        });
        
        return completableFuture;
    }
    
    /**
     * 获取消息统计信息
     */
    public Map<String, Long> getMessageStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalSent", totalMessagesSent.get());
        stats.put("sentSuccess", messagesSentSuccess.get());
        stats.put("sentFailed", messagesSentFailed.get());
        return stats;
    }
    
    /**
     * 重置统计信息
     */
    public void resetStats() {
        totalMessagesSent.set(0);
        messagesSentSuccess.set(0);
        messagesSentFailed.set(0);
    }
    
    /**
     * 生成跟踪ID
     */
    private String generateTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        return traceId;
    }
} 