package com.ww.mall.seckill.manager;

import cn.hutool.core.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2024-04-11- 11:13
 * @description:
 */
@Slf4j
public class MallCacheManager {

    public static void main(String[] args) {
        System.out.println(TimeUnit.SECONDS.name());
    }

    public static final Cache<String, String> spuCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .expireAfterWrite(Duration.ofHours(6))
            .maximumSize(1000)
            .build();

    /**
     * 自定义过期策略
     *
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
            log.info("创建key:【{}】过期时间：【{}】【{}】", key, expireTime, expireTimeUnit.name());
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
