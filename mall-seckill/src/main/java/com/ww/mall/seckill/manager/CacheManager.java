package com.ww.mall.seckill.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;

/**
 * @author ww
 * @create 2024-04-11- 11:13
 * @description:
 */
public class CacheManager {

    public static final Cache<String, String> spuCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .expireAfterWrite(Duration.ofHours(6))
            .maximumSize(1000)
            .build();

}
