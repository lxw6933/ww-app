package com.ww.app.redis.component.pvuv.config;

import com.ww.app.redis.component.pvuv.RedisPvUvComponent;
import com.ww.app.redis.component.pvuv.keys.PvUvRedisKeyBuilder;
import com.ww.app.redis.component.pvuv.storage.RedisPvUvStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis PV/UV自动配置类
 */
@Slf4j
@Configuration
@ConditionalOnClass(StringRedisTemplate.class)
public class RedisPvUvAutoConfiguration {

    /**
     * 记录创建的管理器实例，用于关闭时同步数据
     */
    private final Set<RedisPvUvComponent> managers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * 创建PvUv Redis键构建器
     *
     * @return PvUv Redis键构建器
     */
    @Bean
    public PvUvRedisKeyBuilder pvUvRedisKeyBuilder() {
        return new PvUvRedisKeyBuilder();
    }

    /**
     * 创建Redis PV/UV存储
     *
     * @param redisTemplate Redis模板
     * @return Redis PV/UV存储
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisPvUvStorage redisPvUvStorage(StringRedisTemplate redisTemplate) {
        return new RedisPvUvStorage(redisTemplate);
    }

    /**
     * 创建Redis PV/UV管理器
     *
     * @param redisPvUvStorage Redis PV/UV存储
     * @param pvUvRedisKeyBuilder Redis键构建器
     * @return Redis PV/UV管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisPvUvComponent redisPvUvManager(RedisPvUvStorage redisPvUvStorage, PvUvRedisKeyBuilder pvUvRedisKeyBuilder) {
        RedisPvUvComponent manager = new RedisPvUvComponent(redisPvUvStorage, pvUvRedisKeyBuilder);
        managers.add(manager);
        log.info("初始化Redis PV/UV统计组件成功");
        return manager;
    }

    /**
     * 应用关闭时同步数据
     */
    @PreDestroy
    public void destroy() {
        if (!managers.isEmpty()) {
            log.info("应用关闭，正在同步PV/UV数据到Redis...");
            for (RedisPvUvComponent manager : managers) {
                try {
                    manager.shutdown();
                } catch (Exception e) {
                    log.error("同步PV/UV数据到Redis失败: {}", e.getMessage(), e);
                }
            }
            log.info("PV/UV数据同步完成");
        }
    }
} 