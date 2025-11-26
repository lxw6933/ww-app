package com.ww.app.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.ww.app.common.utils.json.JacksonUtils;
import com.ww.app.redis.AppRedisTemplate;
import com.ww.app.redis.aspect.RateLimitAspect;
import com.ww.app.redis.aspect.ResubmissionAspect;
import com.ww.app.redis.component.ShortCodeRedisComponent;
import com.ww.app.redis.component.key.*;
import com.ww.app.redis.component.lua.RedisScriptComponent;
import com.ww.app.redis.component.rank.RankingRedisComponent;
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
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
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
        // value序列化方式采用GenericJackson2JsonRedisSerializer（支持类型信息）
        template.setValueSerializer(valueSerializer());
        // hash的value序列化方式采用GenericJackson2JsonRedisSerializer（支持类型信息）
        template.setHashValueSerializer(valueSerializer());
        template.afterPropertiesSet();
        log.info("初始化RedisTemplate成功，使用GenericJackson2JsonRedisSerializer支持类型信息...");
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

    /**
     * 值序列化器
     * 使用GenericJackson2JsonRedisSerializer，会在JSON中保存类型信息（@class字段）
     * 这样反序列化时能够恢复原始类型，避免LinkedHashMap类型转换错误
     * <p>
     * 注意：GenericJackson2JsonRedisSerializer内部会创建自己的ObjectMapper并配置类型信息
     * 如果需要使用项目统一的ObjectMapper配置，可以创建自定义序列化器
     * 
     * @return 值序列化器
     */
    private GenericJackson2JsonRedisSerializer valueSerializer() {
        // GenericJackson2JsonRedisSerializer会在JSON中添加@class字段保存类型信息
        // 反序列化时可以根据@class字段恢复原始类型
        return new GenericJackson2JsonRedisSerializer(createObjectMapperWithTypeInfo());
    }
    
    /**
     * 创建带类型信息的ObjectMapper
     * 用于GenericJackson2JsonRedisSerializer，确保序列化时保存类型信息
     * 
     * @return 配置好的ObjectMapper
     */
    private ObjectMapper createObjectMapperWithTypeInfo() {
        // 复制项目统一的ObjectMapper配置
        ObjectMapper objectMapper = JacksonUtils.getObjectMapper().copy();
        
        // 启用默认类型信息，这样序列化时会在JSON中添加@class字段
        // NON_FINAL表示所有非final类都会添加类型信息
        // PROPERTY表示类型信息作为JSON的一个属性（@class字段）存储
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
        );
        
        // 确保所有字段可见（包括private字段）
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        
        return objectMapper;
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

    // ==================================component====================================

    @Bean
    public ShortCodeRedisComponent shortCodeRedisComponent() {
        return new ShortCodeRedisComponent();
    }

    @Bean
    public RedisScriptComponent redisScriptComponent() {
        return new RedisScriptComponent();
    }

    @Bean
    public RankingRedisComponent rankingRedisComponent() {
        return new RankingRedisComponent();
    }

    // ==================================key builder====================================

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
    public RateLimitRedisKeyBuilder rateLimitRedisKeyBuilder() {
        return new RateLimitRedisKeyBuilder();
    }

    @Bean
    public RankingRedisKeyBuilder rankingRedisKeyBuilder() {
        return new RankingRedisKeyBuilder();
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
