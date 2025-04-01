package com.ww.app.common.utils;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.*;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author ww
 * @create 2024-04-19- 10:58
 * @description: Caffeine缓存工具类
 */
@Slf4j
public class CaffeineUtil {

    private final static Integer DEFAULT_SIZE = 100;
    private final static Integer DEFAULT_EXPIRE_TIME = 30;
    private final static TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MINUTES;
    private final static Integer DEFAULT_REFRESH_TIME = 10;

    private CaffeineUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 创建基础缓存
     * 使用默认配置：初始容量100，最大容量100，过期时间30分钟
     */
    public static <K, V> Cache<K, V> createCache() {
        return createCache(DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_EXPIRE_TIME, DEFAULT_TIME_UNIT);
    }

    /**
     * 创建可自动刷新的缓存
     * 使用默认配置：初始容量100，最大容量100，过期时间30分钟，刷新时间10分钟
     */
    public static <K, V> LoadingCache<K, V> createAutoRefreshCache(Function<K, V> refreshFactory) {
        return createAutoRefreshCache(DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_EXPIRE_TIME, DEFAULT_TIME_UNIT,
                DEFAULT_REFRESH_TIME, DEFAULT_TIME_UNIT, refreshFactory);
    }

    /**
     * 创建基础缓存
     *
     * @param initialCapacity 初始容量
     * @param maximumSize 最大容量
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     */
    public static <K, V> Cache<K, V> createCache(Integer initialCapacity,
                                                Integer maximumSize,
                                                Integer expireTime,
                                                TimeUnit timeUnit) {
        return commonCaffeine(initialCapacity, maximumSize, expireTime, expireTime, timeUnit)
                .build();
    }

    /**
     * 创建随机过期时间的缓存
     *
     * @param initialCapacity 初始容量
     * @param maximumSize 最大容量
     * @param minExpireTime 最小过期时间
     * @param maxExpireTime 最大过期时间
     * @param timeUnit 时间单位
     */
    public static <K, V> Cache<K, V> createRandomExpireCache(Integer initialCapacity,
                                                            Integer maximumSize,
                                                            Integer minExpireTime,
                                                            Integer maxExpireTime,
                                                            TimeUnit timeUnit) {
        return commonCaffeine(initialCapacity, maximumSize, minExpireTime, maxExpireTime, timeUnit)
                .build();
    }

    /**
     * 创建可自动刷新的缓存
     *
     * @param initialCapacity 初始容量
     * @param maximumSize 最大容量
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     * @param refreshTime 刷新时间
     * @param refreshTimeUnit 刷新时间单位
     * @param refreshFactory 刷新工厂
     */
    public static <K, V> LoadingCache<K, V> createAutoRefreshCache(Integer initialCapacity,
                                                                  Integer maximumSize,
                                                                  Integer expireTime,
                                                                  TimeUnit timeUnit,
                                                                  Integer refreshTime,
                                                                  TimeUnit refreshTimeUnit,
                                                                  Function<K, V> refreshFactory) {
        return commonCaffeine(initialCapacity, maximumSize, expireTime, expireTime, timeUnit)
                .refreshAfterWrite(refreshTime, refreshTimeUnit)
                .build(getSyncCacheLoader(refreshFactory));
    }

    /**
     * 创建异步自动刷新的缓存
     *
     * @param initialCapacity 初始容量
     * @param maximumSize 最大容量
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     * @param refreshTime 刷新时间
     * @param refreshTimeUnit 刷新时间单位
     * @param refreshFactory 刷新工厂
     * @param executor 执行器
     */
    public static <K, V> AsyncLoadingCache<K, V> createAsyncAutoRefreshCache(Integer initialCapacity,
                                                                           Integer maximumSize,
                                                                           Integer expireTime,
                                                                           TimeUnit timeUnit,
                                                                           Integer refreshTime,
                                                                           TimeUnit refreshTimeUnit,
                                                                           Function<K, V> refreshFactory,
                                                                           Executor executor) {
        return commonCaffeine(initialCapacity, maximumSize, expireTime, expireTime, timeUnit)
                .refreshAfterWrite(refreshTime, refreshTimeUnit)
                .buildAsync(getAsyncCacheLoader(refreshFactory));
    }

    /**
     * 创建随机过期时间且可自动刷新的缓存
     *
     * @param initialCapacity 初始容量
     * @param maximumSize 最大容量
     * @param minExpireTime 最小过期时间
     * @param maxExpireTime 最大过期时间
     * @param timeUnit 时间单位
     * @param refreshTime 刷新时间
     * @param refreshTimeUnit 刷新时间单位
     * @param refreshFactory 刷新工厂
     */
    public static <K, V> LoadingCache<K, V> createRandomExpireAutoRefreshCache(Integer initialCapacity,
                                                                             Integer maximumSize,
                                                                             Integer minExpireTime,
                                                                             Integer maxExpireTime,
                                                                             TimeUnit timeUnit,
                                                                             Integer refreshTime,
                                                                             TimeUnit refreshTimeUnit,
                                                                             Function<K, V> refreshFactory) {
        return commonCaffeine(initialCapacity, maximumSize, minExpireTime, maxExpireTime, timeUnit)
                .refreshAfterWrite(refreshTime, refreshTimeUnit)
                .build(getSyncCacheLoader(refreshFactory));
    }

    /**
     * 通用Caffeine构建器
     */
    private static <K, V> Caffeine<K, V> commonCaffeine(Integer initialCapacity,
                                                        Integer maximumSize,
                                                        Integer minExpireTime,
                                                        Integer maxExpireTime,
                                                        TimeUnit timeUnit) {
        Caffeine<K, V> caffeine = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .removalListener((RemovalListener<K, V>) (key, value, cause) -> {
                    if (log.isDebugEnabled()) {
                        log.debug("缓存移除 - 键: {}, 值: {}, 原因: {}", 
                                key, JSON.toJSONString(value), cause);
                    }
                })
                .maximumSize(maximumSize);

        if (Objects.equals(minExpireTime, maxExpireTime)) {
            caffeine.expireAfterWrite(minExpireTime, timeUnit);
        } else {
            caffeine.expireAfter(new RandomExpiry<>(minExpireTime, maxExpireTime, timeUnit));
        }

        return caffeine;
    }

    /**
     * 同步缓存加载器
     */
    private static <K, V> CacheLoader<K, V> getSyncCacheLoader(Function<K, V> refreshFactory) {
        return new CacheLoader<K, V>() {
            @Override
            public @Nullable V load(@NonNull K key) throws Exception {
                V value = refreshFactory.apply(key);
                if (log.isDebugEnabled()) {
                    log.debug("同步加载缓存 - 键: {}, 值: {}", key, value);
                }
                return value;
            }

            @Override
            public @Nullable V reload(@NonNull K key, @NonNull V oldValue) throws Exception {
                V value = refreshFactory.apply(key);
                if (log.isDebugEnabled()) {
                    log.debug("同步刷新缓存 - 键: {}, 旧值: {}, 新值: {}", 
                            key, oldValue, value);
                }
                return value;
            }
        };
    }

    /**
     * 异步缓存加载器
     */
    private static <K, V> CacheLoader<K, V> getAsyncCacheLoader(Function<K, V> refreshFactory) {
        return new CacheLoader<K, V>() {
            @Override
            public @Nullable V load(@NonNull K key) throws Exception {
                V value = refreshFactory.apply(key);
                if (log.isDebugEnabled()) {
                    log.debug("异步加载缓存 - 键: {}, 值: {}", key, value);
                }
                return value;
            }

            @Override
            public @Nullable V reload(@NonNull K key, @NonNull V oldValue) throws Exception {
                V value = refreshFactory.apply(key);
                if (log.isDebugEnabled()) {
                    log.debug("异步刷新缓存 - 键: {}, 旧值: {}, 新值: {}", 
                            key, oldValue, value);
                }
                return value;
            }
        };
    }

    /**
     * 随机过期策略实现
     */
    private static class RandomExpiry<K, V> implements Expiry<K, V> {
        private final Integer minExpirationTime;
        private final Integer maxExpirationTime;
        private final TimeUnit timeUnit;

        private RandomExpiry(Integer minExpireTime, Integer maxExpireTime, TimeUnit timeUnit) {
            this.minExpirationTime = minExpireTime;
            this.maxExpirationTime = maxExpireTime;
            this.timeUnit = timeUnit;
        }

        @Override
        public long expireAfterCreate(@NonNull K key, @NonNull V value, long currentTime) {
            int expireTime = RandomUtil.randomInt(minExpirationTime, maxExpirationTime);
            if (log.isDebugEnabled()) {
                log.debug("创建缓存项 - 键: {}, 过期时间: {} {}", 
                        key, expireTime, timeUnit.name());
            }
            return timeUnit.toNanos(expireTime);
        }

        @Override
        public long expireAfterUpdate(@NonNull K key, @NonNull V value, long currentTime, @NonNegative long currentDuration) {
            return currentDuration;
        }

        @Override
        public long expireAfterRead(@NonNull K key, @NonNull V value, long currentTime, @NonNegative long currentDuration) {
            return currentDuration;
        }
    }
}
