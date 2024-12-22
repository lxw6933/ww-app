package com.ww.mall.common.utils;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.*;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author ww
 * @create 2024-04-19- 10:58
 * @description: caffeine工具类
 */
@Slf4j
public class MallCaffeineUtil {

    private final static Integer DEFAULT_SIZE = 100;
    private final static Integer DEFAULT_EXPIRE_TIME = 30;
    private final static TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MINUTES;
    private final static Integer DEFAULT_REFRESH_TIME = 10;

    private MallCaffeineUtil() {}

    public static <K, V> Cache<K, V> initCaffeine() {
        return commonCaffeine(0, DEFAULT_SIZE, DEFAULT_EXPIRE_TIME, DEFAULT_EXPIRE_TIME, DEFAULT_TIME_UNIT).build();
    }

    public static <K, V> LoadingCache<K, V> initAutoSyncRefreshCaffeine(Function<K, V> refreshFactory) {
        return commonCaffeine(0, DEFAULT_SIZE, DEFAULT_EXPIRE_TIME, DEFAULT_EXPIRE_TIME, DEFAULT_TIME_UNIT)
                .refreshAfterWrite(DEFAULT_REFRESH_TIME, DEFAULT_TIME_UNIT)
                .build(getSyncCacheLoader(refreshFactory));
    }

    public static <K, V> Cache<K, V> initCaffeine(Integer minSize,
                                                  Integer maxSize,
                                                  Integer expireTime,
                                                  TimeUnit expireTimeUnit) {
        return commonCaffeine(minSize, maxSize, expireTime, expireTime, expireTimeUnit).build();
    }

    public static <K, V> Cache<K, V> initRandomExpireCaffeine(Integer minSize,
                                                              Integer maxSize,
                                                              Integer minExpireTime,
                                                              Integer maxExpireTime,
                                                              TimeUnit expireTimeUnit) {
        return commonCaffeine(minSize, maxSize, minExpireTime, maxExpireTime, expireTimeUnit).build();
    }

    /**
     * 自动刷新caffeine数据
     *
     * @param minSize 缓存初始化大小
     * @param maxSize 缓存最大存储个数
     * @param expireTime 缓存过期时间
     * @param expireTimeUnit 缓存过期时间单位
     * @param refreshTime 自动刷新时间
     * @param refreshTimeUnit 自动刷新时间单位
     * @param refreshFactory 缓存数据工厂
     * @return LoadingCache
     * @param <K> key
     * @param <V> value
     */
    public static <K, V> LoadingCache<K, V> initAutoSyncRefreshCaffeine(Integer minSize,
                                                             Integer maxSize,
                                                             Integer expireTime,
                                                             TimeUnit expireTimeUnit,
                                                             Integer refreshTime,
                                                             TimeUnit refreshTimeUnit,
                                                             Function<K, V> refreshFactory) {
        return commonCaffeine(minSize, maxSize, expireTime, expireTime, expireTimeUnit)
                .refreshAfterWrite(refreshTime, refreshTimeUnit)
                .build(getSyncCacheLoader(refreshFactory));
    }

    public static <K, V> LoadingCache<K, V> initRandomExpireAutoSyncRefreshCaffeine(Integer minSize,
                                                                        Integer maxSize,
                                                                        Integer minExpireTime,
                                                                        Integer maxExpireTime,
                                                                        TimeUnit expireTimeUnit,
                                                                        Integer refreshTime,
                                                                        TimeUnit refreshTimeUnit,
                                                                        Function<K, V> refreshFactory) {
        return commonCaffeine(minSize, maxSize, minExpireTime, maxExpireTime, expireTimeUnit)
                .refreshAfterWrite(refreshTime, refreshTimeUnit)
                .build(getSyncCacheLoader(refreshFactory));
    }

    /**
     * 通用caffeine
     *
     * @param minSize 缓存初始化大小
     * @param maxSize 缓存最大存储个数
     * @param minExpireTime 最小过期时间
     * @param maxExpireTime 最大过期时间
     * @param expireTimeUnit 过期时间单位
     * @return Caffeine
     * @param <K> key
     * @param <V> value
     */
    private static <K, V> Caffeine<K, V> commonCaffeine(Integer minSize,
                                                        Integer maxSize,
                                                        Integer minExpireTime,
                                                        Integer maxExpireTime,
                                                        TimeUnit expireTimeUnit) {
        Caffeine<K, V> caffeine = Caffeine.newBuilder()
                .initialCapacity(minSize)
                .removalListener((RemovalListener<K, V>) (key, value, cause) -> log.info("caffeine remove key:[{}]value:[{}]cause:[{}]", key, JSON.toJSONString(value), cause))
                .maximumSize(maxSize);
        if (Objects.equals(minExpireTime, maxExpireTime)) {
            caffeine.expireAfterWrite(minExpireTime, expireTimeUnit);
        } else {
            caffeine.expireAfter(new MallDefaultExpiry<>(minExpireTime, maxExpireTime, expireTimeUnit));
        }
        return caffeine;
    }

    /**
     * 同步自动刷新加载器
     *
     * @param refreshFactory 刷新工厂
     * @return CacheLoader
     * @param <K> key
     * @param <V> value
     */
    static private <K, V> CacheLoader<K, V> getSyncCacheLoader(Function<K, V> refreshFactory) {
        return new CacheLoader<K, V>() {
            @Override
            public @Nullable V load(@NonNull K key) throws Exception {
                V v = refreshFactory.apply(key);
                log.info("query database data key:[{}]value:[{}]", key, v);
                return v;
            }

            @Override
            public @Nullable V reload(@NonNull K key, @NonNull V oldValue) throws Exception {
                V v = refreshFactory.apply(key);
                log.info("reload database data key:[{}]value:[{}]", key, v);
                return v;
            }
        };
    }

    /**
     * 异步自动刷新加载器
     *
     * @param refreshFactory 刷新工厂
     * @return AsyncCacheLoader
     * @param <K> key
     * @param <V> value
     */
    static private <K, V> AsyncCacheLoader<K, V> getAsyncCacheLoader(Function<K, V> refreshFactory) {
        return new AsyncCacheLoader<K, V>() {

            @Override
            public @NonNull CompletableFuture<V> asyncLoad(@NonNull K key, @NonNull Executor executor) {
                return CompletableFuture.supplyAsync(() -> refreshFactory.apply(key), executor);
            }

            @Override
            public @NonNull CompletableFuture<V> asyncReload(@NonNull K key, @NonNull V oldValue, @NonNull Executor executor) {
                return CompletableFuture.supplyAsync(() -> refreshFactory.apply(key), executor);
            }
        };
    }

    /**
     * 自定义过期策略
     */
    static class MallDefaultExpiry<K, V> implements Expiry<K, V> {
        private final Integer minExpirationTime;
        private final Integer maxExpirationTime;
        private final TimeUnit expireTimeUnit;

        private MallDefaultExpiry(Integer minExpireTime, Integer maxExpireTime, TimeUnit expireTimeUnit) {
            this.minExpirationTime = minExpireTime;
            this.maxExpirationTime = maxExpireTime;
            this.expireTimeUnit = expireTimeUnit;
        }

        @Override
        public long expireAfterCreate(@NonNull K key, @NonNull V value, long currentTime) {
            int expireTime = RandomUtil.randomInt(minExpirationTime, maxExpirationTime);
            log.info("创建key:[{}]过期时间：[{}][{}]", key, expireTime, expireTimeUnit.name());
            return expireTimeUnit.toNanos(expireTime);
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
