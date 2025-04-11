package com.ww.mall.kafka.service;

import com.ww.mall.kafka.exception.KafkaException;
import com.ww.mall.kafka.properties.KafkaProperties;
import com.ww.mall.kafka.template.KafkaOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Kafka服务类，提供高级别的消息处理功能
 */
public class KafkaService {
    private static final Logger log = LoggerFactory.getLogger(KafkaService.class);
    
    private final KafkaOperations kafkaOperations;
    private final KafkaProperties kafkaProperties;
    private ThreadPoolTaskScheduler scheduler;
    private volatile boolean schedulerInitialized = false;
    
    /**
     * 超时时间默认值（毫秒）
     */
    private static final long DEFAULT_TIMEOUT = 10000;
    
    /**
     * 延迟消息头信息
     */
    private static final String SCHEDULED_TIME_HEADER = "X-Scheduled-Time";
    private static final String DELAY_MS_HEADER = "X-Delay-Ms";
    
    /**
     * 构造方法
     */
    public KafkaService(KafkaOperations kafkaOperations) {
        this(kafkaOperations, null);
    }
    
    /**
     * 带配置的构造方法
     */
    public KafkaService(KafkaOperations kafkaOperations, KafkaProperties kafkaProperties) {
        this.kafkaOperations = kafkaOperations;
        this.kafkaProperties = kafkaProperties;
    }
    
    /**
     * 创建并初始化调度器（懒加载）
     */
    private synchronized void initSchedulerIfNeeded() {
        if (!schedulerInitialized) {
            this.scheduler = createScheduler();
            
            // 如果配置不为空，使用配置的值
            if (kafkaProperties != null && kafkaProperties.getScheduler() != null) {
                KafkaProperties.Scheduler schedulerConfig = kafkaProperties.getScheduler();
                scheduler.setPoolSize(schedulerConfig.getPoolSize());
                scheduler.setThreadNamePrefix(schedulerConfig.getThreadNamePrefix());
                scheduler.setDaemon(schedulerConfig.isDaemon());
                // 注: ThreadPoolTaskScheduler 不支持设置队列容量
            } else {
                // 默认值
                scheduler.setPoolSize(5);
                scheduler.setThreadNamePrefix("kafka-delayed-sender-");
                scheduler.setDaemon(true);
            }
            scheduler.setWaitForTasksToCompleteOnShutdown(true);
            scheduler.setAwaitTerminationSeconds(10);
            scheduler.afterPropertiesSet();
            scheduler.initialize();
            schedulerInitialized = true;
            
            log.info("已初始化Kafka调度器: 线程池大小={}, 线程名前缀={}",
                     scheduler.getPoolSize(), 
                     scheduler.getThreadNamePrefix());
        }
    }
    
    /**
     * 创建调度器
     * 便于测试时重写
     */
    protected ThreadPoolTaskScheduler createScheduler() {
        return new ThreadPoolTaskScheduler();
    }
    
    /**
     * 获取调度器
     * 用于测试
     */
    protected ThreadPoolTaskScheduler getScheduler() {
        initSchedulerIfNeeded();
        return scheduler;
    }
    
    /**
     * 获取超时时间
     */
    private long getSendTimeout() {
        return kafkaProperties != null ? kafkaProperties.getSendTimeout() : DEFAULT_TIMEOUT;
    }
    
    /**
     * 获取跟踪ID的头信息名称
     */
    private String getTraceIdHeaderName() {
        if (kafkaProperties != null && kafkaProperties.getTracing() != null) {
            return kafkaProperties.getTracing().getTraceIdHeaderName();
        }
        return "X-Trace-Id";
    }
    
    /**
     * 发送消息
     */
    public CompletableFuture<SendResult<String, String>> sendMessage(String topic, String message) {
        return kafkaOperations.send(topic, message);
    }
    
    /**
     * 发送消息（带Key）
     */
    public CompletableFuture<SendResult<String, String>> sendMessage(String topic, String key, String message) {
        return kafkaOperations.send(topic, key, message);
    }
    
    /**
     * 发送带头信息的消息
     */
    public CompletableFuture<SendResult<String, String>> sendMessage(String topic, String key, String message, Map<String, String> headers) {
        return kafkaOperations.send(topic, key, message, headers);
    }
    
    /**
     * 发送消息到指定分区
     */
    public CompletableFuture<SendResult<String, String>> sendMessage(String topic, Integer partition, String key, String message) {
        return kafkaOperations.send(topic, partition, key, message);
    }
    
    /**
     * 发送JSON对象
     */
    public CompletableFuture<SendResult<String, Object>> sendObject(String topic, Object object) {
        return kafkaOperations.sendJson(topic, object);
    }
    
    /**
     * 发送JSON对象（带Key）
     */
    public CompletableFuture<SendResult<String, Object>> sendObject(String topic, String key, Object object) {
        return kafkaOperations.sendJson(topic, key, object);
    }
    
    /**
     * 发送JSON对象到指定分区
     */
    public CompletableFuture<SendResult<String, Object>> sendObject(String topic, Integer partition, String key, Object object) {
        return kafkaOperations.sendJson(topic, partition, key, object);
    }
    
    /**
     * 同步发送消息
     */
    public SendResult<String, String> sendMessageSync(String topic, String message) {
        try {
            return kafkaOperations.sendSync(topic, message, getSendTimeout());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("同步发送消息失败: {}", e.getMessage(), e);
            throw new KafkaException("发送消息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 同步发送消息（带Key）
     */
    public SendResult<String, String> sendMessageSync(String topic, String key, String message) {
        try {
            return kafkaOperations.sendSync(topic, key, message, getSendTimeout());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("同步发送消息失败: {}", e.getMessage(), e);
            throw new KafkaException("发送消息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 同步发送JSON对象
     */
    public SendResult<String, Object> sendObjectSync(String topic, Object object) {
        try {
            return kafkaOperations.sendJsonSync(topic, object, getSendTimeout());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("同步发送对象失败: {}", e.getMessage(), e);
            throw new KafkaException("发送对象失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 同步发送JSON对象（带Key）
     */
    public SendResult<String, Object> sendObjectSync(String topic, String key, Object object) {
        try {
            return kafkaOperations.sendJsonSync(topic, key, object, getSendTimeout());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("同步发送对象失败: {}", e.getMessage(), e);
            throw new KafkaException("发送对象失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 批量发送消息
     */
    public List<CompletableFuture<SendResult<String, String>>> sendBatch(String topic, List<String> messages) {
        return kafkaOperations.sendBatch(topic, messages);
    }
    
    /**
     * 批量发送消息（带Key）
     */
    public List<CompletableFuture<SendResult<String, String>>> sendBatch(String topic, String key, List<String> messages) {
        return kafkaOperations.sendBatch(topic, key, messages);
    }
    
    /**
     * 异步发送消息并处理回调
     */
    public void sendAsync(String topic, String message, BiConsumer<SendResult<String, String>, Throwable> callback) {
        kafkaOperations.sendAsync(topic, message, callback);
    }
    
    /**
     * 在事务中执行操作
     */
    public <T> T executeInTransaction(KafkaOperations.KafkaTransactionCallback<T> callback) {
        return kafkaOperations.executeInTransaction(callback);
    }
    
    /**
     * 批量发送消息并等待所有消息完成
     */
    public List<SendResult<String, String>> sendBatchAndWait(String topic, List<String> messages) {
        List<CompletableFuture<SendResult<String, String>>> futures = sendBatch(topic, messages);
        
        try {
            // 等待所有消息发送完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(getSendTimeout(), TimeUnit.MILLISECONDS);
            
            // 收集结果
            return futures.stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception e) {
                            throw new RuntimeException("获取发送结果失败", e);
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new KafkaException("批量发送消息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 发送延迟消息
     */
    public ScheduledFuture<?> sendDelayedMessage(String topic, String message, long delayMillis) {
        if (delayMillis <= 0) {
            sendMessage(topic, message);
            return null;
        }
        
        initSchedulerIfNeeded();
        
        return scheduler.schedule(() -> {
            try {
                Map<String, String> headers = new HashMap<>();
                headers.put(DELAY_MS_HEADER, String.valueOf(delayMillis));
                sendMessage(topic, null, message, headers);
            } catch (Exception e) {
                log.error("发送延迟消息失败: {}", e.getMessage(), e);
            }
        }, Instant.now().plusMillis(delayMillis));
    }
    
    /**
     * 发送延迟消息（带key）
     */
    public ScheduledFuture<?> sendDelayedMessage(String topic, String key, String message, long delayMillis) {
        if (delayMillis <= 0) {
            sendMessage(topic, key, message);
            return null;
        }
        
        initSchedulerIfNeeded();
        
        return scheduler.schedule(() -> {
            try {
                Map<String, String> headers = new HashMap<>();
                headers.put(DELAY_MS_HEADER, String.valueOf(delayMillis));
                sendMessage(topic, key, message, headers);
            } catch (Exception e) {
                log.error("发送延迟消息失败: {}", e.getMessage(), e);
            }
        }, Instant.now().plusMillis(delayMillis));
    }
    
    /**
     * 在指定时间发送消息
     */
    public ScheduledFuture<?> sendAtTime(String topic, String message, Date scheduledTime) {
        initSchedulerIfNeeded();
        
        return scheduler.schedule(() -> {
            try {
                Map<String, String> headers = new HashMap<>();
                headers.put(SCHEDULED_TIME_HEADER, String.valueOf(scheduledTime.getTime()));
                sendMessage(topic, null, message, headers);
            } catch (Exception e) {
                log.error("定时消息发送失败: {}", e.getMessage(), e);
            }
        }, scheduledTime);
    }
    
    /**
     * 在指定时间发送消息（带key）
     */
    public ScheduledFuture<?> sendAtTime(String topic, String key, String message, Date scheduledTime) {
        initSchedulerIfNeeded();
        
        return scheduler.schedule(() -> {
            try {
                Map<String, String> headers = new HashMap<>();
                headers.put(SCHEDULED_TIME_HEADER, String.valueOf(scheduledTime.getTime()));
                sendMessage(topic, key, message, headers);
            } catch (Exception e) {
                log.error("定时消息发送失败: {}", e.getMessage(), e);
            }
        }, scheduledTime);
    }
    
    /**
     * 订阅消息
     * 在程序中订阅Kafka主题消息，可用于非Spring管理的组件
     * 
     * @param topic 主题
     * @param groupId 消费者组ID
     * @param messageHandler 消息处理器
     * @return 返回取消订阅的Runnable
     */
    public Runnable subscribe(String topic, String groupId, Consumer<String> messageHandler) {
        KafkaTemplate<String, String> template = kafkaOperations.getStringTemplate();
        
        // 使用内部监听器实现订阅功能
        InternalMessageListener listener = new InternalMessageListener(messageHandler);
        
        // 这里使用一个标记来控制订阅状态
        final boolean[] active = {true};
        
        // 启动监听线程
        Thread listenerThread = new Thread(() -> {
            try {
                log.info("开始订阅Kafka主题: {}, 消费者组: {}", topic, groupId);
                
                // 配置消费者属性
                Map<String, Object> consumerProps = new HashMap<>();
                consumerProps.put("bootstrap.servers", template.getProducerFactory().getConfigurationProperties().get("bootstrap.servers"));
                consumerProps.put("group.id", groupId);
                consumerProps.put("auto.offset.reset", "earliest");
                consumerProps.put("enable.auto.commit", "true");
                consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
                consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
                
                // 创建消费者并订阅
                org.apache.kafka.clients.consumer.KafkaConsumer<String, String> consumer = 
                    new org.apache.kafka.clients.consumer.KafkaConsumer<>(consumerProps);
                consumer.subscribe(Collections.singletonList(topic));
                
                try {
                    while (active[0]) {
                        consumer.poll(java.time.Duration.ofMillis(100))
                               .forEach(record -> {
                                   if (active[0]) {
                                       listener.onMessage(record);
                                   }
                               });
                    }
                } finally {
                    try {
                        consumer.close();
                        log.info("已关闭Kafka消费者: {}, 消费者组: {}", topic, groupId);
                    } catch (Exception e) {
                        log.warn("关闭Kafka消费者时发生错误: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Kafka订阅线程发生异常: {}", e.getMessage(), e);
            }
        });
        
        // 设置为守护线程，避免阻止JVM退出
        listenerThread.setDaemon(true);
        listenerThread.setName("kafka-subscriber-" + topic + "-" + groupId);
        listenerThread.start();
        
        // 返回取消订阅的方法
        return () -> {
            log.info("取消订阅Kafka主题: {}, 消费者组: {}", topic, groupId);
            active[0] = false;
        };
    }
    
    /**
     * 内部消息监听器
     */
    private static class InternalMessageListener implements MessageListener<String, String> {
        private final Consumer<String> messageHandler;
        
        public InternalMessageListener(Consumer<String> messageHandler) {
            this.messageHandler = messageHandler;
        }
        
        @Override
        public void onMessage(org.apache.kafka.clients.consumer.ConsumerRecord<String, String> record) {
            try {
                messageHandler.accept(record.value());
            } catch (Exception e) {
                LoggerFactory.getLogger(InternalMessageListener.class)
                    .error("处理Kafka消息时发生异常: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * 获取KafkaOperations实例
     */
    public KafkaOperations getKafkaOperations() {
        return kafkaOperations;
    }
    
    /**
     * 是否支持事务
     */
    public boolean isTransactional() {
        return kafkaOperations.isTransactional();
    }
    
    /**
     * 关闭服务
     */
    public void shutdown() {
        if (schedulerInitialized && scheduler != null) {
            try {
                log.info("正在关闭Kafka调度器...");
                scheduler.shutdown();
            } catch (Exception e) {
                log.warn("关闭Kafka调度器时出现异常: {}", e.getMessage());
            }
        }
    }
} 