package com.ww.mall.kafka.config;

import com.ww.mall.kafka.properties.KafkaProperties;
import com.ww.mall.kafka.service.KafkaService;
import com.ww.mall.kafka.template.KafkaOperations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Kafka自动配置类
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.kafka", name = "bootstrap-servers")
@EnableConfigurationProperties(KafkaProperties.class)
@Import({KafkaProducerConfig.class, KafkaConsumerConfig.class})
public class KafkaAutoConfiguration {

    /**
     * 创建自定义Kafka操作类
     * 使用正确的类型接收模板实例
     */
    @Bean
    @ConditionalOnMissingBean
    public KafkaOperations kafkaOperations(KafkaTemplate<String, String> stringTemplate, KafkaTemplate<String, Object> jsonTemplate) {
        return new KafkaOperations(stringTemplate, jsonTemplate);
    }

    /**
     * 创建Kafka服务类
     */
    @Bean
    @ConditionalOnMissingBean
    public KafkaService kafkaService(KafkaOperations kafkaOperations, KafkaProperties kafkaProperties) {
        return new KafkaService(kafkaOperations, kafkaProperties);
    }

}
