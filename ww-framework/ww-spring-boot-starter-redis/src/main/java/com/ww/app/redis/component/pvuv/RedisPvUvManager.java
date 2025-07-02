package com.ww.app.redis.component.pvuv;

import com.ww.app.redis.component.pvuv.keys.PvUvRedisKeyBuilder;
import com.ww.app.redis.component.pvuv.storage.LocalPvUvCache;
import com.ww.app.redis.component.pvuv.storage.RedisPvUvStorage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Redis PV/UV统计管理器
 * 提供PV/UV记录和查询功能，采用本地缓存+Redis存储
 */
@Slf4j
public class RedisPvUvManager {

    /**
     * 默认的计数值
     */
    private static final Long DEFAULT_COUNT = 0L;

    /**
     * 默认的PV/UV结果
     */
    private static final PvUvResult DEFAULT_RESULT = new PvUvResult(DEFAULT_COUNT, DEFAULT_COUNT);

    /**
     * 用于异步任务的线程池
     */
    private final ScheduledExecutorService scheduledExecutorService;

    /**
     * 本地PV/UV缓存
     */
    private final LocalPvUvCache localCache;

    /**
     * Redis PV/UV存储
     */
    private final RedisPvUvStorage redisStorage;

    /**
     * Redis键构建器
     */
    private final PvUvRedisKeyBuilder keyBuilder;

    /**
     * 是否正在执行同步
     */
    private final AtomicBoolean isSyncing = new AtomicBoolean(false);

    /**
     * 构造函数
     *
     * @param redisStorage Redis存储
     * @param keyBuilder   Redis键构建器
     */
    public RedisPvUvManager(RedisPvUvStorage redisStorage, PvUvRedisKeyBuilder keyBuilder) {
        this.redisStorage = redisStorage;
        this.keyBuilder = keyBuilder;
        this.localCache = new LocalPvUvCache();
        this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
        // 开启定时任务
        scheduledExecutorService.scheduleAtFixedRate(this::scheduledSyncToRedis, 0, 30, TimeUnit.SECONDS);

        log.info("RedisPvUvManager初始化成功，数据同步间隔: 30秒");
    }

    /**
     * 记录PV
     *
     * @param key 业务键
     * @return 当前PV值
     */
    public long recordPv(String key) {
        return recordPv(key, null);
    }

    /**
     * 记录PV
     *
     * @param key  业务键
     * @param date 日期，null表示当天
     * @return 当前PV值
     */
    public long recordPv(String key, LocalDate date) {
        return validateAndExecute(key, "记录PV", () -> {
            String pvKey = keyBuilder.buildPvKey(key, date);
            return localCache.incrementPv(pvKey);
        }, DEFAULT_COUNT);
    }

    /**
     * 记录UV
     *
     * @param key    业务键
     * @param userId 用户标识
     * @return 当前UV值
     */
    public long recordUv(String key, String userId) {
        return recordUv(key, userId, null);
    }

    /**
     * 记录UV
     *
     * @param key    业务键
     * @param userId 用户标识
     * @param date   日期，null表示当天
     * @return 当前UV值
     */
    public long recordUv(String key, String userId, LocalDate date) {
        return validateAndExecute(key, "记录UV", () -> {
            // 处理无效用户ID
            if (userId == null || userId.isEmpty()) {
                // 默认不记录异常用户ID
                return getUv(key, date);
            }

            String uvKey = keyBuilder.buildUvKey(key, date);
            localCache.addUserToUv(uvKey, userId);
            return localCache.getUvCount(uvKey);
        }, DEFAULT_COUNT);
    }

    /**
     * 同时记录PV和UV
     *
     * @param key    业务键
     * @param userId 用户标识
     * @return PV/UV结果
     */
    public PvUvResult recordPvAndUv(String key, String userId) {
        return recordPvAndUv(key, userId, null);
    }

    /**
     * 同时记录PV和UV
     *
     * @param key    业务键
     * @param userId 用户标识
     * @param date   日期，null表示当天
     * @return PV/UV结果
     */
    public PvUvResult recordPvAndUv(String key, String userId, LocalDate date) {
        return validateAndExecute(key, "记录PV/UV", () -> {
            long pv = recordPv(key, date);
            long uv = recordUv(key, userId, date);
            return new PvUvResult(pv, uv);
        }, DEFAULT_RESULT);
    }

    /**
     * 记录活动的PV和UV
     *
     * @param eventId 活动ID
     * @param userId  用户标识
     * @return PV/UV结果
     */
    public PvUvResult recordEventPvAndUv(String eventId, String userId) {
        return recordEventPvAndUv(eventId, userId, null);
    }

    /**
     * 记录活动的PV和UV
     *
     * @param eventId 活动ID
     * @param userId  用户标识
     * @param date    日期，null表示不区分日期的全局活动统计
     * @return PV/UV结果
     */
    public PvUvResult recordEventPvAndUv(String eventId, String userId, LocalDate date) {
        return validateAndExecute(eventId, "记录活动PV/UV", () -> {
            String eventKey = keyBuilder.buildEventKey(eventId);
            return recordPvAndUv(eventKey, userId, date);
        }, DEFAULT_RESULT);
    }

    /**
     * 获取PV值
     *
     * @param key 业务键
     * @return PV值
     */
    public long getPv(String key) {
        return getPv(key, null);
    }

    /**
     * 获取PV值
     * 注意：查询方法直接访问Redis，Redis失败直接返回0
     *
     * @param key  业务键
     * @param date 日期，null表示当天
     * @return PV值
     */
    public long getPv(String key, LocalDate date) {
        return validateAndExecute(key, "获取PV", () -> {
            String pvKey = keyBuilder.buildPvKey(key, date);

            try {
                return redisStorage.getPv(pvKey);
            } catch (Exception e) {
                log.error("从Redis获取PV失败: {}", e.getMessage(), e);
                return DEFAULT_COUNT;
            }
        }, DEFAULT_COUNT);
    }

    /**
     * 获取UV值
     *
     * @param key 业务键
     * @return UV值
     */
    public long getUv(String key) {
        return getUv(key, null);
    }

    /**
     * 获取UV值
     * 注意：查询方法直接访问Redis，Redis失败直接返回0
     *
     * @param key  业务键
     * @param date 日期，null表示当天
     * @return UV值
     */
    public long getUv(String key, LocalDate date) {
        return validateAndExecute(key, "获取UV", () -> {
            String uvKey = keyBuilder.buildUvKey(key, date);

            try {
                return redisStorage.getUvCount(uvKey);
            } catch (Exception e) {
                log.error("从Redis获取UV失败: {}", e.getMessage(), e);
                return DEFAULT_COUNT;
            }
        }, DEFAULT_COUNT);
    }

    /**
     * 同时获取PV和UV值
     *
     * @param key 业务键
     * @return PV/UV结果
     */
    public PvUvResult getPvAndUv(String key) {
        return getPvAndUv(key, null);
    }

    /**
     * 同时获取PV和UV值
     *
     * @param key  业务键
     * @param date 日期，null表示当天
     * @return PV/UV结果
     */
    public PvUvResult getPvAndUv(String key, LocalDate date) {
        return validateAndExecute(key, "获取PV/UV", () -> {
            long pv = getPv(key, date);
            long uv = getUv(key, date);
            return new PvUvResult(pv, uv);
        }, DEFAULT_RESULT);
    }

    /**
     * 强制将本地缓存同步到Redis
     * 优化: 使用LongAdder.sumThenReset和双缓冲交换实现原子读取和重置
     */
    public void syncToRedisNow() {
        if (isSyncing.compareAndSet(false, true)) {
            try {
                log.debug("开始同步PV/UV数据到Redis...");
                long startTime = System.currentTimeMillis();

                Map<String, Long> pvData = localCache.getAllPv();

                Map<String, Set<String>> uvData = localCache.getAllUvUsers();

                // 将数据同步到Redis（仅当有数据时）
                if (!pvData.isEmpty()) {
                    syncPvToRedis(pvData);
                }

                if (!uvData.isEmpty()) {
                    syncUvToRedis(uvData);
                }

                long duration = System.currentTimeMillis() - startTime;
                log.debug("同步PV/UV数据到Redis完成，耗时：{}ms", duration);
            } catch (Exception e) {
                log.error("同步PV/UV数据到Redis异常: {}", e.getMessage(), e);
            } finally {
                isSyncing.set(false);
            }
        } else {
            log.debug("已有同步任务正在执行，忽略本次同步请求");
        }
    }

    /**
     * 定时任务：将本地缓存同步到Redis
     */
    public void scheduledSyncToRedis() {
        // 只有当缓存中有数据时才同步
        if (localCache.getPvCacheSize() > 0 || localCache.getUvCacheSize() > 0) {
            syncToRedisNow();
        }
    }

    /**
     * 同步PV数据到Redis
     *
     * @param pvData PV数据
     */
    private void syncPvToRedis(Map<String, Long> pvData) {
        if (pvData.isEmpty()) {
            return;
        }

        try {
            // 批量更新Redis中的PV
            redisStorage.batchIncrementPv(pvData);
            log.debug("成功同步{}个PV数据到Redis", pvData.size());
        } catch (Exception e) {
            log.error("同步PV数据到Redis失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 同步UV数据到Redis
     *
     * @param uvData UV数据
     */
    private void syncUvToRedis(Map<String, Set<String>> uvData) {
        if (uvData.isEmpty()) {
            return;
        }

        try {
            // 批量更新Redis中的UV
            redisStorage.batchAddUsersToUv(uvData);
            log.debug("成功同步{}个UV数据到Redis", uvData.size());
        } catch (Exception e) {
            log.error("同步UV数据到Redis失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 通用参数校验和执行方法（带默认值）
     *
     * @param key          需要校验的键
     * @param operation    操作名称（用于日志）
     * @param supplier     校验通过后执行的函数
     * @param defaultValue 校验失败时返回的默认值
     * @param <T>          返回值类型
     * @return 执行结果或默认值
     */
    private <T> T validateAndExecute(String key, String operation, Supplier<T> supplier, T defaultValue) {
        if (key == null || key.isEmpty()) {
            log.warn("{}时传入的key为空", operation);
            return defaultValue;
        }
        return supplier.get();
    }

    /**
     * 关闭，释放资源
     */
    public void shutdown() {
        try {
            syncToRedisNow();
        } finally {
            scheduledExecutorService.shutdown();
        }
    }

    /**
     * PV/UV结果类
     */
    @Data
    @AllArgsConstructor
    public static class PvUvResult {
        /**
         * PV值
         */
        private final long pv;

        /**
         * UV值
         */
        private final long uv;
    }
} 