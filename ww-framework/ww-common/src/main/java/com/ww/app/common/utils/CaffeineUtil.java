package com.ww.app.common.utils;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.*;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Caffeine缓存工具类
 * <p>
 * 提供多种缓存创建和管理功能：
 * <ul>
 *   <li>基础缓存：支持固定/随机过期时间</li>
 *   <li>自动刷新缓存：同步/异步刷新</li>
 *   <li>批量加载缓存：高效批量操作</li>
 *   <li>缓存统计：监控缓存性能</li>
 *   <li>缓存预热：提前加载热点数据</li>
 * </ul>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * // 1. 创建基础缓存
 * Cache<String, User> cache = CaffeineUtil.createCache();
 *
 * // 2. 创建自动刷新缓存
 * LoadingCache<String, User> loadingCache = CaffeineUtil.createAutoRefreshCache(
 *     key -> userService.getUserById(key)
 * );
 *
 * // 3. 创建批量加载缓存
 * LoadingCache<String, User> batchCache = CaffeineUtil.createBatchLoadingCache(
 *     100, 1000, 30, TimeUnit.MINUTES,
 *     keys -> userService.batchGetUsers(keys)
 * );
 *
 * // 4. 获取缓存统计信息
 * CacheStats stats = CaffeineUtil.getStats(cache);
 * }</pre>
 *
 * @author ww
 * @since 2024-04-19
 */
@Slf4j
public class CaffeineUtil {

    private static final Integer DEFAULT_SIZE = 100;
    private static final Integer DEFAULT_EXPIRE_TIME = 30;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.MINUTES;
    private static final Integer DEFAULT_REFRESH_TIME = 10;
    private static final Integer MIN_CAPACITY = 1;
    private static final Integer MIN_EXPIRE_TIME = 1;

    private CaffeineUtil() {
        throw new IllegalStateException("Utility class");
    }

    // ==================== 基础缓存创建 ====================

    /**
     * 创建基础缓存（使用默认配置）
     * <p>默认配置：初始容量100，最大容量100，过期时间30分钟</p>
     *
     * @param <K> 键类型
     * @param <V> 值类型
     * @return Cache实例
     */
    public static <K, V> Cache<K, V> createCache() {
        return createCache(DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_EXPIRE_TIME, DEFAULT_TIME_UNIT);
    }

    /**
     * 创建基础缓存（自定义配置）
     *
     * @param initialCapacity 初始容量（必须 >= 1）
     * @param maximumSize     最大容量（必须 >= 1）
     * @param expireTime      过期时间（必须 >= 1）
     * @param timeUnit        时间单位（不能为null）
     * @param <K>             键类型
     * @param <V>             值类型
     * @return Cache实例
     * @throws IllegalArgumentException 如果参数不合法
     */
    public static <K, V> Cache<K, V> createCache(Integer initialCapacity,
                                                 Integer maximumSize,
                                                 Integer expireTime,
                                                 TimeUnit timeUnit) {
        validateParams(initialCapacity, maximumSize, expireTime, timeUnit);
        return commonCaffeine(initialCapacity, maximumSize, expireTime, expireTime, timeUnit, null, false)
                .build();
    }

    /**
     * 创建带统计功能的基础缓存
     *
     * @param initialCapacity 初始容量
     * @param maximumSize     最大容量
     * @param expireTime      过期时间
     * @param timeUnit        时间单位
     * @param <K>             键类型
     * @param <V>             值类型
     * @return Cache实例
     */
    public static <K, V> Cache<K, V> createCacheWithStats(Integer initialCapacity,
                                                          Integer maximumSize,
                                                          Integer expireTime,
                                                          TimeUnit timeUnit) {
        validateParams(initialCapacity, maximumSize, expireTime, timeUnit);
        return commonCaffeine(initialCapacity, maximumSize, expireTime, expireTime, timeUnit, null, true)
                .build();
    }

    /**
     * 创建随机过期时间的缓存
     * <p>每个缓存项的过期时间在 [minExpireTime, maxExpireTime] 之间随机，可有效防止缓存雪崩</p>
     *
     * @param initialCapacity 初始容量
     * @param maximumSize     最大容量
     * @param minExpireTime   最小过期时间（必须 >= 1）
     * @param maxExpireTime   最大过期时间（必须 > minExpireTime）
     * @param timeUnit        时间单位
     * @param <K>             键类型
     * @param <V>             值类型
     * @return Cache实例
     * @throws IllegalArgumentException 如果 maxExpireTime <= minExpireTime
     */
    public static <K, V> Cache<K, V> createRandomExpireCache(Integer initialCapacity,
                                                             Integer maximumSize,
                                                             Integer minExpireTime,
                                                             Integer maxExpireTime,
                                                             TimeUnit timeUnit) {
        validateParams(initialCapacity, maximumSize, minExpireTime, timeUnit);
        validateExpireTimeRange(minExpireTime, maxExpireTime);
        return commonCaffeine(initialCapacity, maximumSize, minExpireTime, maxExpireTime, timeUnit, null, false)
                .build();
    }

    // ==================== 自动刷新缓存 ====================

    /**
     * 创建可自动刷新的缓存（使用默认配置）
     * <p>默认配置：初始容量100，最大容量100，过期时间30分钟，刷新时间10分钟</p>
     *
     * @param refreshFactory 刷新工厂函数
     * @param <K>            键类型
     * @param <V>            值类型
     * @return LoadingCache实例
     * @throws IllegalArgumentException 如果refreshFactory为null
     */
    public static <K, V> LoadingCache<K, V> createAutoRefreshCache(Function<K, V> refreshFactory) {
        validateRefreshFactory(refreshFactory);
        return createAutoRefreshCache(DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_EXPIRE_TIME, DEFAULT_TIME_UNIT,
                DEFAULT_REFRESH_TIME, DEFAULT_TIME_UNIT, refreshFactory);
    }

    /**
     * 创建可自动刷新的缓存（自定义配置）
     *
     * @param initialCapacity 初始容量
     * @param maximumSize     最大容量
     * @param expireTime      过期时间
     * @param timeUnit        时间单位
     * @param refreshTime     刷新时间（建议小于expireTime）
     * @param refreshTimeUnit 刷新时间单位
     * @param refreshFactory  刷新工厂函数
     * @param <K>             键类型
     * @param <V>             值类型
     * @return LoadingCache实例
     */
    public static <K, V> LoadingCache<K, V> createAutoRefreshCache(Integer initialCapacity,
                                                                   Integer maximumSize,
                                                                   Integer expireTime,
                                                                   TimeUnit timeUnit,
                                                                   Integer refreshTime,
                                                                   TimeUnit refreshTimeUnit,
                                                                   Function<K, V> refreshFactory) {
        validateParams(initialCapacity, maximumSize, expireTime, timeUnit);
        validateRefreshParams(refreshTime, refreshTimeUnit, refreshFactory);
        return commonCaffeine(initialCapacity, maximumSize, expireTime, expireTime, timeUnit, null, false)
                .refreshAfterWrite(refreshTime, refreshTimeUnit)
                .build(createCacheLoader(refreshFactory, "同步"));
    }

    /**
     * 创建异步自动刷新的缓存
     * <p>刷新操作在指定的线程池中异步执行，不会阻塞主线程</p>
     *
     * @param initialCapacity 初始容量
     * @param maximumSize     最大容量
     * @param expireTime      过期时间
     * @param timeUnit        时间单位
     * @param refreshTime     刷新时间
     * @param refreshTimeUnit 刷新时间单位
     * @param refreshFactory  刷新工厂函数
     * @param executor        执行器（用于异步刷新）
     * @param <K>             键类型
     * @param <V>             值类型
     * @return AsyncLoadingCache实例
     */
    public static <K, V> AsyncLoadingCache<K, V> createAsyncAutoRefreshCache(Integer initialCapacity,
                                                                             Integer maximumSize,
                                                                             Integer expireTime,
                                                                             TimeUnit timeUnit,
                                                                             Integer refreshTime,
                                                                             TimeUnit refreshTimeUnit,
                                                                             Function<K, V> refreshFactory,
                                                                             Executor executor) {
        validateParams(initialCapacity, maximumSize, expireTime, timeUnit);
        validateRefreshParams(refreshTime, refreshTimeUnit, refreshFactory);
        validateExecutor(executor);
        return commonCaffeine(initialCapacity, maximumSize, expireTime, expireTime, timeUnit, null, false)
                .refreshAfterWrite(refreshTime, refreshTimeUnit)
                .executor(executor)
                .buildAsync(createCacheLoader(refreshFactory, "异步"));
    }

    /**
     * 创建随机过期时间且可自动刷新的缓存
     *
     * @param initialCapacity 初始容量
     * @param maximumSize     最大容量
     * @param minExpireTime   最小过期时间
     * @param maxExpireTime   最大过期时间
     * @param timeUnit        时间单位
     * @param refreshTime     刷新时间
     * @param refreshTimeUnit 刷新时间单位
     * @param refreshFactory  刷新工厂函数
     * @param <K>             键类型
     * @param <V>             值类型
     * @return LoadingCache实例
     */
    public static <K, V> LoadingCache<K, V> createRandomExpireAutoRefreshCache(Integer initialCapacity,
                                                                               Integer maximumSize,
                                                                               Integer minExpireTime,
                                                                               Integer maxExpireTime,
                                                                               TimeUnit timeUnit,
                                                                               Integer refreshTime,
                                                                               TimeUnit refreshTimeUnit,
                                                                               Function<K, V> refreshFactory) {
        validateParams(initialCapacity, maximumSize, minExpireTime, timeUnit);
        validateExpireTimeRange(minExpireTime, maxExpireTime);
        validateRefreshParams(refreshTime, refreshTimeUnit, refreshFactory);
        return commonCaffeine(initialCapacity, maximumSize, minExpireTime, maxExpireTime, timeUnit, null, false)
                .refreshAfterWrite(refreshTime, refreshTimeUnit)
                .build(createCacheLoader(refreshFactory, "随机过期同步"));
    }

    // ==================== 批量加载缓存 ====================

    /**
     * 创建支持批量加载的缓存
     * <p>批量加载可以显著提升性能，适合需要从数据库或远程服务批量获取数据的场景</p>
     *
     * @param initialCapacity   初始容量
     * @param maximumSize       最大容量
     * @param expireTime        过期时间
     * @param timeUnit          时间单位
     * @param batchLoadFunction 批量加载函数（输入键集合，返回键值映射）
     * @param <K>               键类型
     * @param <V>               值类型
     * @return LoadingCache实例
     * @throws IllegalArgumentException 如果batchLoadFunction为null
     */
    public static <K, V> LoadingCache<K, V> createBatchLoadingCache(Integer initialCapacity,
                                                                    Integer maximumSize,
                                                                    Integer expireTime,
                                                                    TimeUnit timeUnit,
                                                                    Function<Collection<K>, Map<K, V>> batchLoadFunction) {
        validateParams(initialCapacity, maximumSize, expireTime, timeUnit);
        validateBatchLoadFunction(batchLoadFunction);
        return commonCaffeine(initialCapacity, maximumSize, expireTime, expireTime, timeUnit, null, false)
                .build(createBatchCacheLoader(batchLoadFunction));
    }

    /**
     * 创建支持批量加载和自动刷新的缓存
     *
     * @param initialCapacity   初始容量
     * @param maximumSize       最大容量
     * @param expireTime        过期时间
     * @param timeUnit          时间单位
     * @param refreshTime       刷新时间
     * @param refreshTimeUnit   刷新时间单位
     * @param batchLoadFunction 批量加载函数
     * @param <K>               键类型
     * @param <V>               值类型
     * @return LoadingCache实例
     */
    public static <K, V> LoadingCache<K, V> createBatchAutoRefreshCache(Integer initialCapacity,
                                                                        Integer maximumSize,
                                                                        Integer expireTime,
                                                                        TimeUnit timeUnit,
                                                                        Integer refreshTime,
                                                                        TimeUnit refreshTimeUnit,
                                                                        Function<Collection<K>, Map<K, V>> batchLoadFunction) {
        validateParams(initialCapacity, maximumSize, expireTime, timeUnit);
        validateRefreshParams(refreshTime, refreshTimeUnit, null);
        validateBatchLoadFunction(batchLoadFunction);
        return commonCaffeine(initialCapacity, maximumSize, expireTime, expireTime, timeUnit, null, false)
                .refreshAfterWrite(refreshTime, refreshTimeUnit)
                .build(createBatchCacheLoader(batchLoadFunction));
    }

    // ==================== 自定义配置缓存 ====================

    /**
     * 创建带自定义移除监听器的缓存
     *
     * @param initialCapacity 初始容量
     * @param maximumSize     最大容量
     * @param expireTime      过期时间
     * @param timeUnit        时间单位
     * @param removalListener 移除监听器
     * @param <K>             键类型
     * @param <V>             值类型
     * @return Cache实例
     */
    public static <K, V> Cache<K, V> createCacheWithListener(Integer initialCapacity,
                                                             Integer maximumSize,
                                                             Integer expireTime,
                                                             TimeUnit timeUnit,
                                                             RemovalListener<K, V> removalListener) {
        validateParams(initialCapacity, maximumSize, expireTime, timeUnit);
        Objects.requireNonNull(removalListener, "removalListener不能为null");
        return commonCaffeine(initialCapacity, maximumSize, expireTime, expireTime, timeUnit, removalListener, false)
                .build();
    }

    // ==================== 缓存操作方法 ====================

    /**
     * 批量获取缓存值
     * <p>如果缓存中不存在，则通过CacheLoader批量加载</p>
     *
     * @param cache 缓存实例
     * @param keys  键集合
     * @param <K>   键类型
     * @param <V>   值类型
     * @return 值映射（失败时返回空Map）
     */
    public static <K, V> Map<K, V> getAll(LoadingCache<K, V> cache, Collection<K> keys) {
        if (cache == null || keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return cache.getAll(keys);
        } catch (Exception e) {
            log.error("批量获取缓存失败 - 键数量: {}, 错误: {}", keys.size(), e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * 安全获取单个缓存值
     *
     * @param cache        缓存实例
     * @param key          键
     * @param defaultValue 默认值（当获取失败时返回）
     * @param <K>          键类型
     * @param <V>          值类型
     * @return 缓存值或默认值
     */
    public static <K, V> V getOrDefault(LoadingCache<K, V> cache, K key, V defaultValue) {
        if (cache == null || key == null) {
            return defaultValue;
        }
        try {
            V value = cache.get(key);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            log.error("获取缓存失败 - 键: {}, 错误: {}", key, e.getMessage());
            return defaultValue;
        }
    }

    /**
     * 批量刷新缓存
     * <p>异步刷新指定键的缓存值</p>
     *
     * @param cache 缓存实例
     * @param keys  键集合
     * @param <K>   键类型
     * @param <V>   值类型
     */
    public static <K, V> void refreshAll(LoadingCache<K, V> cache, Collection<K> keys) {
        if (cache == null || keys == null || keys.isEmpty()) {
            return;
        }
        try {
            keys.forEach(key -> {
                try {
                    cache.refresh(key);
                } catch (Exception e) {
                    log.error("刷新缓存失败 - 键: {}, 错误: {}", key, e.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("批量刷新缓存失败 - 键数量: {}, 错误: {}", keys.size(), e.getMessage(), e);
        }
    }

    /**
     * 批量刷新缓存（并发执行）
     *
     * @param cache 缓存实例
     * @param keys  键集合
     * @param <K>   键类型
     * @param <V>   值类型
     * @return CompletableFuture（所有刷新操作完成后complete）
     */
    public static <K, V> CompletableFuture<Void> refreshAllAsync(LoadingCache<K, V> cache, Collection<K> keys) {
        if (cache == null || keys == null || keys.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (K key : keys) {
            futures.add(CompletableFuture.runAsync(() -> cache.refresh(key)));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * 缓存预热
     * <p>提前加载热点数据到缓存中</p>
     *
     * @param cache         缓存实例
     * @param warmUpDataMap 预热数据映射
     * @param <K>           键类型
     * @param <V>           值类型
     */
    public static <K, V> void warmUp(Cache<K, V> cache, Map<K, V> warmUpDataMap) {
        if (cache == null || warmUpDataMap == null || warmUpDataMap.isEmpty()) {
            return;
        }
        try {
            cache.putAll(warmUpDataMap);
            log.info("缓存预热完成 - 预热数据量: {}", warmUpDataMap.size());
        } catch (Exception e) {
            log.error("缓存预热失败 - 数据量: {}, 错误: {}", warmUpDataMap.size(), e.getMessage(), e);
        }
    }

    /**
     * 批量删除缓存
     *
     * @param cache 缓存实例
     * @param keys  键集合
     * @param <K>   键类型
     * @param <V>   值类型
     */
    public static <K, V> void invalidateAll(Cache<K, V> cache, Collection<K> keys) {
        if (cache == null || keys == null || keys.isEmpty()) {
            return;
        }
        try {
            cache.invalidateAll(keys);
        } catch (Exception e) {
            log.error("批量删除缓存失败 - 键数量: {}, 错误: {}", keys.size(), e.getMessage(), e);
        }
    }

    /**
     * 清空所有缓存
     *
     * @param cache 缓存实例
     * @param <K>   键类型
     * @param <V>   值类型
     */
    public static <K, V> void clear(Cache<K, V> cache) {
        if (cache == null) {
            return;
        }
        try {
            cache.invalidateAll();
            log.info("缓存已清空");
        } catch (Exception e) {
            log.error("清空缓存失败 - 错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 清理过期缓存
     *
     * @param cache 缓存实例
     * @param <K>   键类型
     * @param <V>   值类型
     */
    public static <K, V> void cleanUp(Cache<K, V> cache) {
        if (cache == null) {
            return;
        }
        try {
            cache.cleanUp();
        } catch (Exception e) {
            log.error("清理过期缓存失败 - 错误: {}", e.getMessage(), e);
        }
    }

    // ==================== 缓存统计方法 ====================

    /**
     * 获取缓存统计信息
     *
     * @param cache 缓存实例
     * @param <K>   键类型
     * @param <V>   值类型
     * @return CacheStats 统计信息（如果不支持则返回null）
     */
    public static <K, V> CacheStats getStats(Cache<K, V> cache) {
        if (cache == null) {
            return null;
        }
        try {
            return cache.stats();
        } catch (Exception e) {
            log.error("获取缓存统计信息失败 - 错误: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取缓存大小
     *
     * @param cache 缓存实例
     * @param <K>   键类型
     * @param <V>   值类型
     * @return 缓存中的条目数量
     */
    public static <K, V> long size(Cache<K, V> cache) {
        if (cache == null) {
            return 0L;
        }
        try {
            return cache.estimatedSize();
        } catch (Exception e) {
            log.error("获取缓存大小失败 - 错误: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 打印缓存统计信息
     *
     * @param cache     缓存实例
     * @param cacheName 缓存名称
     * @param <K>       键类型
     * @param <V>       值类型
     */
    public static <K, V> void logStats(Cache<K, V> cache, String cacheName) {
        if (cache == null) {
            return;
        }
        try {
            CacheStats stats = cache.stats();
            log.info("缓存统计 [{}] - 命中率: {}%, 命中次数: {}, 未命中次数: {}, 加载成功次数: {}, " +
                            "加载失败次数: {}, 加载总耗时: {}ms, 驱逐次数: {}, 当前大小: {}",
                    cacheName,
                    String.format("%.2f", stats.hitRate() * 100),
                    stats.hitCount(),
                    stats.missCount(),
                    stats.loadSuccessCount(),
                    stats.loadFailureCount(),
                    stats.totalLoadTime() / 1_000_000,
                    stats.evictionCount(),
                    cache.estimatedSize()
            );
        } catch (Exception e) {
            log.error("打印缓存统计信息失败 - 缓存名称: {}, 错误: {}", cacheName, e.getMessage());
        }
    }

    /**
     * 获取缓存命中率
     *
     * @param cache 缓存实例
     * @param <K>   键类型
     * @param <V>   值类型
     * @return 命中率（0.0 - 1.0），失败返回-1
     */
    public static <K, V> double getHitRate(Cache<K, V> cache) {
        if (cache == null) {
            return -1;
        }
        try {
            CacheStats stats = cache.stats();
            return stats.hitRate();
        } catch (Exception e) {
            log.error("获取缓存命中率失败 - 错误: {}", e.getMessage());
            return -1;
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 通用Caffeine构建器
     */
    private static <K, V> Caffeine<K, V> commonCaffeine(Integer initialCapacity,
                                                        Integer maximumSize,
                                                        Integer minExpireTime,
                                                        Integer maxExpireTime,
                                                        TimeUnit timeUnit,
                                                        RemovalListener<K, V> customListener,
                                                        boolean recordStats) {
        Caffeine<K, V> builder = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)
                .removalListener(customListener != null ?
                        customListener :
                        (RemovalListener<K, V>) (key, value, cause) -> {
                            if (log.isDebugEnabled()) {
                                log.debug("缓存移除 - 键: {}, 值: {}, 原因: {}",
                                        key, JSON.toJSONString(value), cause);
                            }
                        })
                .maximumSize(maximumSize);

        // 配置过期策略
        if (Objects.equals(minExpireTime, maxExpireTime)) {
            builder.expireAfterWrite(minExpireTime, timeUnit);
        } else {
            builder.expireAfter(new RandomExpiry<>(minExpireTime, maxExpireTime, timeUnit));
        }

        // 是否记录统计信息
        if (recordStats) {
            builder.recordStats();
        }

        return builder;
    }

    /**
     * 创建缓存加载器（统一实现，避免代码重复）
     */
    private static <K, V> CacheLoader<K, V> createCacheLoader(Function<K, V> refreshFactory, String loaderType) {
        return new CacheLoader<K, V>() {
            @Override
            public @Nullable V load(@NonNull K key) throws Exception {
                try {
                    V value = refreshFactory.apply(key);
                    if (log.isDebugEnabled()) {
                        log.debug("{}加载缓存 - 键: {}, 值: {}", loaderType, key, value);
                    }
                    return value;
                } catch (Exception e) {
                    log.error("{}加载缓存失败 - 键: {}, 错误: {}", loaderType, key, e.getMessage(), e);
                    throw e;
                }
            }

            @Override
            public @Nullable V reload(@NonNull K key, @NonNull V oldValue) throws Exception {
                try {
                    V value = refreshFactory.apply(key);
                    if (log.isDebugEnabled()) {
                        log.debug("{}刷新缓存 - 键: {}, 旧值: {}, 新值: {}",
                                loaderType, key, oldValue, value);
                    }
                    return value;
                } catch (Exception e) {
                    log.error("{}刷新缓存失败 - 键: {}, 错误: {}", loaderType, key, e.getMessage(), e);
                    // 刷新失败时返回旧值，保证服务可用性
                    return oldValue;
                }
            }
        };
    }

    /**
     * 创建批量缓存加载器（重写loadAll方法以支持真正的批量加载）
     */
    private static <K, V> CacheLoader<K, V> createBatchCacheLoader(Function<Collection<K>, Map<K, V>> batchLoadFunction) {
        return new CacheLoader<K, V>() {
            @Override
            public @Nullable V load(@NonNull K key) {
                try {
                    Map<K, V> result = batchLoadFunction.apply(Collections.singleton(key));
                    V value = result != null ? result.get(key) : null;
                    if (log.isDebugEnabled()) {
                        log.debug("单个加载缓存 - 键: {}, 值: {}", key, value);
                    }
                    return value;
                } catch (Exception e) {
                    log.error("单个加载缓存失败 - 键: {}, 错误: {}", key, e.getMessage(), e);
                    throw e;
                }
            }

            @Override
            public @NonNull Map<K, V> loadAll(@NonNull Iterable<? extends K> keys) {
                try {
                    List<K> keyList = new ArrayList<>();
                    keys.forEach(keyList::add);
                    
                    if (keyList.isEmpty()) {
                        return Collections.emptyMap();
                    }
                    
                    Map<K, V> result = batchLoadFunction.apply(keyList);
                    if (log.isDebugEnabled()) {
                        log.debug("批量加载缓存 - 键数量: {}, 加载成功数量: {}",
                                keyList.size(), result != null ? result.size() : 0);
                    }
                    return result != null ? result : Collections.emptyMap();
                } catch (Exception e) {
                    log.error("批量加载缓存失败 - 错误: {}", e.getMessage(), e);
                    throw e;
                }
            }

            @Override
            public V reload(@NonNull K key, @NonNull V oldValue) {
                try {
                    Map<K, V> result = batchLoadFunction.apply(Collections.singleton(key));
                    V value = result != null ? result.get(key) : null;
                    if (log.isDebugEnabled()) {
                        log.debug("单个刷新缓存 - 键: {}, 旧值: {}, 新值: {}",
                                key, oldValue, value);
                    }
                    return value != null ? value : oldValue;
                } catch (Exception e) {
                    log.error("单个刷新缓存失败 - 键: {}, 错误: {}", key, e.getMessage(), e);
                    return oldValue;
                }
            }
        };
    }

    // ==================== 参数验证方法 ====================

    /**
     * 验证基础参数
     */
    private static void validateParams(Integer initialCapacity, Integer maximumSize,
                                       Integer expireTime, TimeUnit timeUnit) {
        if (initialCapacity == null || initialCapacity < MIN_CAPACITY) {
            throw new IllegalArgumentException("初始容量必须 >= " + MIN_CAPACITY);
        }
        if (maximumSize == null || maximumSize < MIN_CAPACITY) {
            throw new IllegalArgumentException("最大容量必须 >= " + MIN_CAPACITY);
        }
        if (initialCapacity > maximumSize) {
            throw new IllegalArgumentException("初始容量不能大于最大容量");
        }
        if (expireTime == null || expireTime < MIN_EXPIRE_TIME) {
            throw new IllegalArgumentException("过期时间必须 >= " + MIN_EXPIRE_TIME);
        }
        if (timeUnit == null) {
            throw new IllegalArgumentException("时间单位不能为null");
        }
    }

    /**
     * 验证过期时间范围
     */
    private static void validateExpireTimeRange(Integer minExpireTime, Integer maxExpireTime) {
        if (maxExpireTime == null || maxExpireTime <= minExpireTime) {
            throw new IllegalArgumentException("最大过期时间必须大于最小过期时间");
        }
    }

    /**
     * 验证刷新参数
     */
    private static void validateRefreshParams(Integer refreshTime, TimeUnit refreshTimeUnit,
                                              Function<?, ?> refreshFactory) {
        if (refreshTime == null || refreshTime < MIN_EXPIRE_TIME) {
            throw new IllegalArgumentException("刷新时间必须 >= " + MIN_EXPIRE_TIME);
        }
        if (refreshTimeUnit == null) {
            throw new IllegalArgumentException("刷新时间单位不能为null");
        }
        if (refreshFactory != null) {
            validateRefreshFactory(refreshFactory);
        }
    }

    /**
     * 验证刷新工厂
     */
    private static void validateRefreshFactory(Function<?, ?> refreshFactory) {
        if (refreshFactory == null) {
            throw new IllegalArgumentException("刷新工厂函数不能为null");
        }
    }

    /**
     * 验证批量加载函数
     */
    private static void validateBatchLoadFunction(Function<?, ?> batchLoadFunction) {
        if (batchLoadFunction == null) {
            throw new IllegalArgumentException("批量加载函数不能为null");
        }
    }

    /**
     * 验证执行器
     */
    private static void validateExecutor(Executor executor) {
        if (executor == null) {
            throw new IllegalArgumentException("执行器不能为null");
        }
    }

    // ==================== 内部类 ====================

    /**
     * 随机过期策略实现
     * <p>为每个缓存项生成随机过期时间，有效防止缓存雪崩</p>
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
            int expireTime = RandomUtil.randomInt(minExpirationTime, maxExpirationTime + 1);
            if (log.isDebugEnabled()) {
                log.debug("创建缓存项 - 键: {}, 过期时间: {} {}",
                        key, expireTime, timeUnit.name());
            }
            return timeUnit.toNanos(expireTime);
        }

        @Override
        public long expireAfterUpdate(@NonNull K key, @NonNull V value, long currentTime,
                                      @NonNegative long currentDuration) {
            // 更新后保持原有的过期时间
            return currentDuration;
        }

        @Override
        public long expireAfterRead(@NonNull K key, @NonNull V value, long currentTime,
                                    @NonNegative long currentDuration) {
            // 读取后保持原有的过期时间
            return currentDuration;
        }
    }
}
