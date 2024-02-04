package com.ww.mall.redis;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ww.mall.redis.aspect.MallRateLimitAspect;
import com.ww.mall.redis.aspect.MallResubmissionAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author ww
 * @create 2023-07-15- 15:18
 * @description:
 */
@Slf4j
@EnableCaching
@ConditionalOnClass({RedisTemplate.class})
@EnableConfigurationProperties({CacheProperties.class})
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
        log.info("初始化RedisTemplate成功...");
        return template;
    }

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration(CacheProperties cacheProperties) {
        CacheProperties.Redis redisCacheProperties = cacheProperties.getRedis();
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // 缓存key设置序列化类型
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer()))
                // 缓存value设置序列化类型
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer()))
                // 缓存name设置前缀
                .computePrefixWith(cacheName -> "cache:" + cacheName + ":");
        // 是否缓存空值
        if (!redisCacheProperties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        // 是否使用前缀
        if (!redisCacheProperties.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }
        // 默认缓存时间
        if (redisCacheProperties.getTimeToLive() != null) {
            config = config.entryTtl(redisCacheProperties.getTimeToLive());
        }
        // 缓存前缀
        if (redisCacheProperties.getKeyPrefix() != null) {
            config = config.prefixCacheNameWith(redisCacheProperties.getKeyPrefix());
        }
        log.info("初始化RedisCacheManager成功...");
        return config;
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

    @Bean
    public MallResubmissionAspect mallResubmissionAspect() {
        return new MallResubmissionAspect();
    }

    @Bean
    public MallRateLimitAspect mallRateLimitAspect() {
        return new MallRateLimitAspect();
    }

    @Bean
    public MallRedisUtil mallRedisUtil() {
        return new MallRedisUtil();
    }

}
