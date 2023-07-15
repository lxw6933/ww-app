package com.ww.mall.redis;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ww
 * @create 2023-07-15- 15:18
 * @description:
 */
@Slf4j
@Configuration
@ConditionalOnClass({RedisTemplate.class})
@EnableConfigurationProperties({MallRedisCacheProperties.class})
public class MallRedisAutoConfiguration {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        // key采用String的序列化方式
        template.setKeySerializer(keySerializer());
        // hash的key也采用String的序列化方式
        template.setHashKeySerializer(keySerializer());
        // value序列化方式采用jackson
        template.setValueSerializer(valueSerializer());
        // hash的value序列化方式采用jackson
        template.setHashValueSerializer(valueSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate();
        stringRedisTemplate.setConnectionFactory(redisConnectionFactory);
        return stringRedisTemplate;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory, MallRedisCacheProperties redisCacheProperties) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // 缓存key设置序列化类型
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer()))
                // 缓存value设置序列化类型
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer()))
                // 缓存name设置前缀
                .computePrefixWith(cacheName -> "cache:" + cacheName + ":");
        // 是否缓存空值
        if (!redisCacheProperties.getCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        // 默认缓存时间
        if (redisCacheProperties.getDefaultExpiration() != null) {
            config = config.entryTtl(redisCacheProperties.getDefaultExpiration());
        }
        // 自定义不同cacheKey缓存时间
        Map<String, RedisCacheConfiguration> cacheExpiresMap = new HashMap<>();
        if (redisCacheProperties.getExpires() != null && redisCacheProperties.getExpires().size() > 0) {
            for (Map.Entry<String, Duration> entry : redisCacheProperties.getExpires().entrySet()) {
                cacheExpiresMap.put(entry.getKey(), config.entryTtl(entry.getValue()));
            }
        }
        // 使用RedisCacheConfiguration创建RedisCacheManager
        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(redisConnectionFactory).cacheDefaults(config);
        if (cacheExpiresMap.size() > 0) {
            builder = builder.withInitialCacheConfigurations(cacheExpiresMap);
        }
        return builder.build();
    }

    private StringRedisSerializer keySerializer() {
        return new StringRedisSerializer();
    }

    private Jackson2JsonRedisSerializer<Object> valueSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
        return jackson2JsonRedisSerializer;
    }

}
