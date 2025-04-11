package com.ww.mall.kafka.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka消费者配置
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.kafka", name = "bootstrap-servers")
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:default-group}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    @Value("${spring.kafka.consumer.enable-auto-commit:false}")
    private Boolean enableAutoCommit;

    @Value("${spring.kafka.consumer.auto-commit-interval:1000}")
    private Integer autoCommitInterval;

    @Value("${spring.kafka.consumer.max-poll-records:500}")
    private Integer maxPollRecords;

    @Value("${spring.kafka.consumer.max-poll-interval-ms:300000}")
    private Integer maxPollIntervalMs;

    @Value("${spring.kafka.consumer.session-timeout-ms:10000}")
    private Integer sessionTimeoutMs;

    @Value("${spring.kafka.consumer.heartbeat-interval-ms:3000}")
    private Integer heartbeatIntervalMs;

    @Value("${spring.kafka.consumer.concurrency:3}")
    private Integer concurrency;

    @Value("${spring.kafka.consumer.batch-size:100}")
    private Integer batchSize;

    @Value("${spring.kafka.consumer.batch-enabled:false}")
    private Boolean batchEnabled;

    @Value("${spring.kafka.consumer.retry.interval:1000}")
    private Long retryInterval;

    @Value("${spring.kafka.consumer.retry.max-attempts:3}")
    private Integer maxRetryAttempts;
    
    @Value("${spring.kafka.consumer.trusted-packages:*}")
    private String trustedPackages;
    
    @Value("${spring.kafka.consumer.error-handler-enabled:true}")
    private Boolean errorHandlerEnabled;
    
    @Value("${spring.kafka.consumer.safe-deserialization:true}")
    private Boolean safeDeserialization;

    /**
     * 字符串消费者工厂配置
     */
    @Bean
    @ConditionalOnMissingBean(name = "stringConsumerFactory")
    public ConsumerFactory<String, String> stringConsumerFactory() {
        Map<String, Object> configProps = getCommonConsumerConfig();
        
        // 配置反序列化
        if (safeDeserialization) {
            // 使用带错误处理的反序列化器
            configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
            configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
            configProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
            configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, StringDeserializer.class);
        } else {
            configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        }
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * JSON消费者工厂配置
     */
    @Bean
    @ConditionalOnMissingBean(name = "jsonConsumerFactory")
    public ConsumerFactory<String, Object> jsonConsumerFactory() {
        Map<String, Object> configProps = getCommonConsumerConfig();
        
        // 配置Json反序列化
        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>();
        jsonDeserializer.addTrustedPackages(trustedPackages.split(","));
        
        if (safeDeserialization) {
            // 使用带错误处理的反序列化器
            configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
            configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
            configProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
            configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
            configProps.put(JsonDeserializer.TRUSTED_PACKAGES, trustedPackages);
            
            return new DefaultKafkaConsumerFactory<>(configProps);
        } else {
            configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            
            return new DefaultKafkaConsumerFactory<>(
                    configProps,
                    new StringDeserializer(), 
                    jsonDeserializer
            );
        }
    }

    /**
     * 公共消费者配置
     */
    private Map<String, Object> getCommonConsumerConfig() {
        Map<String, Object> configProps = new HashMap<>();
        // 基础配置
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        
        // 消费行为配置
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);
        configProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, autoCommitInterval);
        
        // 性能配置
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, heartbeatIntervalMs);
        
        return configProps;
    }

    /**
     * 字符串消费者监听容器工厂
     */
    @Bean
    @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stringConsumerFactory());
        factory.setConcurrency(concurrency);
        
        configureCommonFactorySettings(factory);
        
        return factory;
    }

    /**
     * JSON消费者监听容器工厂
     */
    @Bean
    @ConditionalOnMissingBean(name = "kafkaJsonListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaJsonListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(jsonConsumerFactory());
        factory.setConcurrency(concurrency);
        
        configureCommonFactorySettings(factory);
        
        return factory;
    }

    /**
     * 批量消费监听容器工厂
     */
    @Bean
    @ConditionalOnMissingBean(name = "batchKafkaListenerContainerFactory")
    @ConditionalOnProperty(prefix = "spring.kafka.consumer", name = "batch-enabled", havingValue = "true")
    public ConcurrentKafkaListenerContainerFactory<String, String> batchKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stringConsumerFactory());
        factory.setConcurrency(concurrency);
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        if (errorHandlerEnabled) {
            factory.setCommonErrorHandler(createErrorHandler());
        }
        
        return factory;
    }
    
    /**
     * 配置通用工厂设置
     */
    private void configureCommonFactorySettings(ConcurrentKafkaListenerContainerFactory<?, ?> factory) {
        // 配置手动确认模式
        if (!enableAutoCommit) {
            factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        }
        
        // 配置批量消费
        if (batchEnabled) {
            factory.setBatchListener(true);
            factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        }
        
        // 配置错误处理器
        if (errorHandlerEnabled) {
            factory.setCommonErrorHandler(createErrorHandler());
        }
    }
    
    /**
     * 创建默认错误处理器
     */
    private DefaultErrorHandler createErrorHandler() {
        // 固定间隔重试策略
        FixedBackOff backOff = new FixedBackOff(retryInterval, maxRetryAttempts);
        
        // 创建错误处理器
        DefaultErrorHandler errorHandler = new DefaultErrorHandler((record, exception) -> {
            // 当超过重试次数后会回调此处理器
            // 可以添加自定义处理逻辑，例如记录错误日志或将消息发送到死信队列
        }, backOff);
        
        // 配置不重试的异常类型
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        
        return errorHandler;
    }
}
