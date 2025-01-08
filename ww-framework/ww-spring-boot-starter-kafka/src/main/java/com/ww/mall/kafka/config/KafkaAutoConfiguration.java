package com.ww.mall.kafka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2025-01-05 13:25
 * @description:
 */
@Configuration
public class KafkaAutoConfiguration {

    @Bean
    public KafkaProducerConfig kafkaProducerConfig() {
        return new KafkaProducerConfig();
    }

    @Bean
    public KafkaConsumerConfig kafkaConsumerConfig() {
        return new KafkaConsumerConfig();
    }

}
