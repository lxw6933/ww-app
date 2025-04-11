package com.ww.mall.kafka.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.transaction.KafkaTransactionManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka生产者配置
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.kafka", name = "bootstrap-servers")
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer.acks:all}")
    private String acks;

    @Value("${spring.kafka.producer.retries:3}")
    private Integer retries;

    @Value("${spring.kafka.producer.batch-size:16384}")
    private Integer batchSize;

    @Value("${spring.kafka.producer.buffer-memory:33554432}")
    private Integer bufferMemory;

    @Value("${spring.kafka.producer.compression-type:none}")
    private String compressionType;

    @Value("${spring.kafka.producer.transaction-id-prefix:tx-}")
    private String transactionIdPrefix;

    @Value("${spring.kafka.producer.enable-idempotence:true}")
    private Boolean enableIdempotence;

    @Value("${spring.kafka.producer.enable-transactions:false}")
    private Boolean enableTransactions;

    /**
     * 创建String类型的生产者工厂
     */
    @Bean
    @ConditionalOnMissingBean(name = "kafkaStringProducerFactory")
    public ProducerFactory<String, String> stringProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        // 基础配置
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // 性能和可靠性配置
        configProps.put(ProducerConfig.ACKS_CONFIG, acks);
        configProps.put(ProducerConfig.RETRIES_CONFIG, retries);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
        
        // 幂等性配置
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, enableIdempotence);
        
        // 如果启用事务，需要配置事务ID前缀
        if (enableTransactions) {
            configProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionIdPrefix);
        }
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * 创建JSON类型的生产者工厂
     */
    @Bean
    @ConditionalOnMissingBean(name = "kafkaJsonProducerFactory")
    public ProducerFactory<String, Object> jsonProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        // 基础配置
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // 性能和可靠性配置
        configProps.put(ProducerConfig.ACKS_CONFIG, acks);
        configProps.put(ProducerConfig.RETRIES_CONFIG, retries);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
        
        // 幂等性配置
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, enableIdempotence);
        
        // 如果启用事务，需要配置事务ID前缀
        if (enableTransactions) {
            configProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionIdPrefix);
        }
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * 创建String类型的KafkaTemplate
     */
    @Bean
    @ConditionalOnMissingBean(name = "kafkaTemplate")
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(stringProducerFactory());
    }

    /**
     * 创建JSON类型的KafkaTemplate
     */
    @Bean
    @ConditionalOnMissingBean(name = "kafkaJsonTemplate")
    public KafkaTemplate<String, Object> kafkaJsonTemplate() {
        return new KafkaTemplate<>(jsonProducerFactory());
    }

    /**
     * 配置事务管理器
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.kafka.producer", name = "enable-transactions", havingValue = "true")
    @ConditionalOnMissingBean(name = "kafkaTransactionManager")
    public KafkaTransactionManager<String, Object> kafkaTransactionManager() {
        return new KafkaTransactionManager<>(jsonProducerFactory());
    }
}
