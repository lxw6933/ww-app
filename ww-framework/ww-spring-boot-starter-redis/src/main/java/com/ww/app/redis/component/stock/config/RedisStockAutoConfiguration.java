package com.ww.app.redis.component.stock.config;

import com.ww.app.redis.component.stock.StockRedisComponent;
import com.ww.app.redis.component.stock.keys.StockRedisKeyBuilder;
import com.ww.app.redis.component.stock.handler.RedisStockHandlerManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author ww
 * @create 2025-07-10- 13:35
 * @description:
 */
@Slf4j
@Configuration
@ConditionalOnClass(StringRedisTemplate.class)
public class RedisStockAutoConfiguration {

    @Bean
    public StockRedisComponent stockRedisComponent() {
        return new StockRedisComponent();
    }

    @Bean
    public StockRedisKeyBuilder stockRedisKeyBuilder() {
        return new StockRedisKeyBuilder();
    }

    @Bean
    public RedisStockHandlerManager redisStockHandlerManager() {
        return new RedisStockHandlerManager();
    }

}
