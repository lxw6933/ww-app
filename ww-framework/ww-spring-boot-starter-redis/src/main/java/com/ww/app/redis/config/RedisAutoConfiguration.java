package com.ww.app.redis.config;

import com.ww.app.common.utils.json.JacksonUtils;
import com.ww.app.redis.AppRedisTemplate;
import com.ww.app.redis.aspect.RateLimitAspect;
import com.ww.app.redis.aspect.ResubmissionAspect;
import com.ww.app.redis.component.ShortCodeRedisComponent;
import com.ww.app.redis.component.StockRedisComponent;
import com.ww.app.redis.component.key.*;
import com.ww.app.redis.component.lua.RedisScriptComponent;
import com.ww.app.redis.handler.RedisStockHandlerManager;
import com.ww.app.redis.listener.RedisChannelListener;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Collection;

/**
 * @author ww
 * @create 2023-07-15- 15:18
 * @description:
 */
@Slf4j
@EnableCaching
@ConditionalOnClass({RedisTemplate.class})
@EnableConfigurationProperties({CacheProperties.class})
public class RedisAutoConfiguration implements ApplicationContextAware {

    private ApplicationContext applicationContext;

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

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        Collection<RedisChannelListener> redisChannelListeners = this.applicationContext.getBeansOfType(RedisChannelListener.class).values();
        redisChannelListeners.forEach(redisChannelListener -> {
            log.info("register redis listener channelName:[{}]", redisChannelListener.channelName());
            container.addMessageListener(redisChannelListener, redisChannelListener.channelTopics());
        });
        return container;
    }

    private StringRedisSerializer keySerializer() {
        return new StringRedisSerializer();
    }

    private Jackson2JsonRedisSerializer<Object> valueSerializer() {
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        jackson2JsonRedisSerializer.setObjectMapper(JacksonUtils.getObjectMapper());
        return jackson2JsonRedisSerializer;
    }

    @Bean
    public ResubmissionAspect mallResubmissionAspect() {
        return new ResubmissionAspect();
    }

    @Bean
    public RateLimitAspect mallRateLimitAspect() {
        return new RateLimitAspect();
    }

    @Bean
    public AppRedisTemplate mallRedisTemplate() {
        return new AppRedisTemplate();
    }

    @Bean
    public StockRedisComponent stockRedisComponent() {
        return new StockRedisComponent();
    }

    @Bean
    public ShortCodeRedisComponent shortCodeRedisComponent() {
        return new ShortCodeRedisComponent();
    }

    @Bean
    public RedisScriptComponent redisScriptComponent() {
        return new RedisScriptComponent();
    }

    @Bean
    public StockRedisKeyBuilder stockRedisKeyBuilder() {
        return new StockRedisKeyBuilder();
    }

    @Bean
    public AppLockRedisKeyBuilder appLockRedisKeyBuilder() {
        return new AppLockRedisKeyBuilder();
    }

    @Bean
    public GeoRedisKeyBuilder geoRedisKeyBuilder() {
        return new GeoRedisKeyBuilder();
    }

    @Bean
    public SpuRedisKeyBuilder spuRedisKeyBuilder() {
        return new SpuRedisKeyBuilder();
    }

    @Bean
    public ShortCodeRedisKeyBuilder shortCodeRedisKeyBuilder() {
        return new ShortCodeRedisKeyBuilder();
    }

    @Bean
    public RedisStockHandlerManager redisStockHandlerManager() {
        return new RedisStockHandlerManager();
    }

    @Bean
    public RateLimitRedisKeyBuilder rateLimitRedisKeyBuilder() {
        return new RateLimitRedisKeyBuilder();
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
