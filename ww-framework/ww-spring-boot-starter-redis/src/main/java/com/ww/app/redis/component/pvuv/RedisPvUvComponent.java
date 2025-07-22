package com.ww.app.redis.component.pvuv;

import com.ww.app.redis.component.pvuv.enums.PvUvBizTypeEnum;
import com.ww.app.redis.component.pvuv.keys.PvUvRedisKeyBuilder;
import com.ww.app.redis.component.pvuv.storage.LocalPvUvCache;
import com.ww.app.redis.component.pvuv.storage.RedisPvUvStorage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.ww.app.common.constant.Constant.SHUTDOWN_TIMEOUT_SECONDS;

/**
 * Redis PV/UV统计管理器
 * 提供PV/UV记录和查询功能，采用本地缓存+Redis存储
 */
@Slf4j
public class RedisPvUvComponent {

    /**
     * 默认的计数值
     */
    private static final Long DEFAULT_COUNT = 0L;

    /**
     * 默认的PV/UV结果
     */
    private static final PvUvResult DEFAULT_RESULT = new PvUvResult(DEFAULT_COUNT, DEFAULT_COUNT);

    // 添加配置参数
    private static final int DEFAULT_SYNC_INTERVAL_SECONDS = 30;

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
    public RedisPvUvComponent(RedisPvUvStorage redisStorage, PvUvRedisKeyBuilder keyBuilder) {
        this(redisStorage, keyBuilder, DEFAULT_SYNC_INTERVAL_SECONDS);
    }

    public RedisPvUvComponent(RedisPvUvStorage redisStorage, PvUvRedisKeyBuilder keyBuilder, int syncIntervalSeconds) {
        this.redisStorage = Objects.requireNonNull(redisStorage, "redisStorage不能为null");
        this.keyBuilder = Objects.requireNonNull(keyBuilder, "keyBuilder不能为null");
        this.localCache = new LocalPvUvCache();

        // 使用带名称的线程工厂
        this.scheduledExecutorService = Executors.newScheduledThreadPool(1,
                r -> new Thread(r, "PvUv-Sync-Thread"));

        // 开启定时任务
        scheduledExecutorService.scheduleAtFixedRate(this::scheduledSyncToRedis,
                0, syncIntervalSeconds, TimeUnit.SECONDS);

        log.info("RedisPvUvManager初始化成功，数据同步间隔: {}秒", syncIntervalSeconds);
    }

    /**
     * 记录PV（带业务类型）
     * 记录指定业务类型下的页面访问量
     *
     * @param bizType 业务类型，如PAGE、PRODUCT、ARTICLE等
     * @param key     业务键，如页面ID、商品ID、文章ID等
     */
    public void recordPv(PvUvBizTypeEnum bizType, String key) {
        recordPv(bizType, key, LocalDate.now());
    }

    /**
     * 记录PV（带业务类型和日期）
     * 记录指定业务类型和日期下的页面访问量
     *
     * @param bizType 业务类型，如PAGE、PRODUCT、ARTICLE等
     * @param key     业务键，如页面ID、商品ID、文章ID等
     * @param date    日期，null表示不区分日期的全局统计
     */
    public void recordPv(PvUvBizTypeEnum bizType, String key, LocalDate date) {
        validateAndExecute(key, () -> {
            String pvKey = keyBuilder.buildPvKey(bizType, key, date);
            localCache.incrementPv(pvKey);
        });
    }

    /**
     * 记录UV（带业务类型）
     * 记录指定业务类型下的独立访客数
     *
     * @param bizType 业务类型，如PAGE、PRODUCT、ARTICLE等
     * @param key     业务键，如页面ID、商品ID、文章ID等
     * @param userId  用户标识，用于UV去重统计
     */
    public void recordUv(PvUvBizTypeEnum bizType, String key, String userId) {
        recordUv(bizType, key, userId, LocalDate.now());
    }

    /**
     * 记录UV（带业务类型和日期）
     * 记录指定业务类型和日期下的独立访客数
     *
     * @param bizType 业务类型，如PAGE、PRODUCT、ARTICLE等
     * @param key     业务键，如页面ID、商品ID、文章ID等
     * @param userId  用户标识，用于UV去重统计
     * @param date    日期，null表示不区分日期的全局统计
     */
    public void recordUv(PvUvBizTypeEnum bizType, String key, String userId, LocalDate date) {
        validateAndExecute(key, () -> {
            // 处理无效用户ID
            if (userId == null || userId.isEmpty()) {
                // 默认不记录异常用户ID
                return;
            }

            String uvKey = keyBuilder.buildUvKey(bizType, key, date);
            localCache.addUserToUv(uvKey, userId);
        });
    }

    /**
     * 同时记录PV和UV（带业务类型）
     * 同时记录指定业务类型下的页面访问量和独立访客数
     *
     * @param bizType 业务类型，如PAGE、PRODUCT、ARTICLE等
     * @param key     业务键，如页面ID、商品ID、文章ID等
     * @param userId  用户标识，用于UV去重统计
     */
    public void recordPvAndUv(PvUvBizTypeEnum bizType, String key, String userId) {
        recordPvAndUv(bizType, key, userId, LocalDate.now());
    }

    /**
     * 同时记录PV和UV（带业务类型和日期）
     * 同时记录指定业务类型和日期下的页面访问量和独立访客数
     *
     * @param bizType 业务类型，如PAGE、PRODUCT、ARTICLE等
     * @param key     业务键，如页面ID、商品ID、文章ID等
     * @param userId  用户标识，用于UV去重统计
     * @param date    日期，null表示不区分日期的全局统计
     */
    public void recordPvAndUv(PvUvBizTypeEnum bizType, String key, String userId, LocalDate date) {
        validateAndExecute(key, () -> {
            recordPv(bizType, key, date);
            recordUv(bizType, key, userId, date);
        });
    }

    /**
     * 记录活动的PV和UV[无日期]
     * 记录活动的全局PV/UV统计（不区分日期）
     *
     * @param bizType 业务类型，如EVENT、PRODUCT等
     * @param key    活动ID
     * @param userId 用户标识
     */
    public void recordTotalPvAndUv(PvUvBizTypeEnum bizType, String key, String userId) {
        // 直接调用无日期版本，实现全局活动统计
        recordPvAndUv(bizType, key, userId, null);
    }

    /**
     * 获取PV值（带业务类型）
     * 获取指定业务类型下的页面访问量
     *
     * @param bizType 业务类型，如PAGE、PRODUCT、ARTICLE等
     * @param key     业务键，如页面ID、商品ID、文章ID等
     * @return PV值
     */
    public long getPv(PvUvBizTypeEnum bizType, String key) {
        return getPv(bizType, key, LocalDate.now());
    }

    /**
     * 获取PV值（带业务类型和日期）
     * 获取指定业务类型和日期下的页面访问量
     *
     * @param bizType 业务类型，如PAGE、PRODUCT、ARTICLE等
     * @param key     业务键，如页面ID、商品ID、文章ID等
     * @param date    日期，null表示不区分日期的全局统计
     * @return PV值
     */
    public long getPv(PvUvBizTypeEnum bizType, String key, LocalDate date) {
        return validateAndExecute(key, () -> {
            String pvKey = keyBuilder.buildPvKey(bizType, key, date);

            try {
                return redisStorage.getPv(pvKey);
            } catch (Exception e) {
                log.error("从Redis获取PV失败: {}", e.getMessage(), e);
                return DEFAULT_COUNT;
            }
        }, DEFAULT_COUNT);
    }

    /**
     * 获取不区分日期的全局PV值
     *
     * @param bizType 业务类型，如PAGE、PRODUCT、ARTICLE等
     * @param key     业务键，如页面ID、商品ID、文章ID等
     * @return PV值
     */
    public long getTotalPv(PvUvBizTypeEnum bizType, String key) {
        return getPv(bizType, key, null);
    }

    /**
     * 获取UV值（带业务类型）
     * 获取指定业务类型下的独立访客数
     *
     * @param bizType 业务类型，如PAGE、PRODUCT、ARTICLE等
     * @param key     业务键，如页面ID、商品ID、文章ID等
     * @return UV值
     */
    public long getUv(PvUvBizTypeEnum bizType, String key) {
        return getUv(bizType, key, LocalDate.now());
    }

    /**
     * 获取UV值（带业务类型和日期）
     * 获取指定业务类型和日期下的独立访客数
     *
     * @param bizType 业务类型，如PAGE、PRODUCT、ARTICLE等
     * @param key     业务键，如页面ID、商品ID、文章ID等
     * @param date    日期，null表示不区分日期的全局统计
     * @return UV值
     */
    public long getUv(PvUvBizTypeEnum bizType, String key, LocalDate date) {
        return validateAndExecute(key, () -> {
            String uvKey = keyBuilder.buildUvKey(bizType, key, date);

            try {
                return redisStorage.getUvCount(uvKey);
            } catch (Exception e) {
                log.error("从Redis获取UV失败: {}", e.getMessage(), e);
                return DEFAULT_COUNT;
            }
        }, DEFAULT_COUNT);
    }

    /**
     * 获取不区分日期的全局UV值
     *
     * @param bizType 业务类型，如PAGE、PRODUCT、ARTICLE等
     * @param key     业务键，如页面ID、商品ID、文章ID等
     * @return UV值
     */
    public long getTotalUv(PvUvBizTypeEnum bizType, String key) {
        return getUv(bizType, key, null);
    }

    /**
     * 同时获取PV和UV值（带业务类型）
     * 获取指定业务类型下的页面访问量和独立访客数
     *
     * @param bizType 业务类型，如PAGE、PRODUCT、ARTICLE等
     * @param key     业务键，如页面ID、商品ID、文章ID等
     * @return PV/UV结果
     */
    public PvUvResult getPvAndUv(PvUvBizTypeEnum bizType, String key) {
        return getPvAndUv(bizType, key, LocalDate.now());
    }

    /**
     * 同时获取PV和UV值（带业务类型和日期）
     * 获取指定业务类型和日期下的页面访问量和独立访客数
     *
     * @param bizType 业务类型，如PAGE、PRODUCT、ARTICLE等
     * @param key     业务键，如页面ID、商品ID、文章ID等
     * @param date    日期，null表示不区分日期的全局统计
     * @return PV/UV结果
     */
    public PvUvResult getPvAndUv(PvUvBizTypeEnum bizType, String key, LocalDate date) {
        return validateAndExecute(key, () -> {
            long pv = getPv(bizType, key, date);
            long uv = getUv(bizType, key, date);
            return new PvUvResult(pv, uv);
        }, DEFAULT_RESULT);
    }

    /**
     * 获取不区分日期的全局PV/UV值
     *
     * @param bizType 业务类型，如PAGE、PRODUCT、ARTICLE等
     * @param key     业务键，如页面ID、商品ID、文章ID等
     * @return PV/UV结果
     */
    public PvUvResult getTotalPvAndUv(PvUvBizTypeEnum bizType, String key) {
        return getPvAndUv(bizType, key, null);
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
     * 获取业务类型描述，用于日志记录
     *
     * @param bizType 业务类型
     * @return 业务类型描述
     */
    private String getBizTypeDesc(PvUvBizTypeEnum bizType) {
        return bizType != null ? "[" + bizType.getDesc() + "]" : "";
    }

    /**
     * 通用参数校验和执行方法（带默认值）
     *
     * @param key          需要校验的键
     * @param supplier     校验通过后执行的函数
     * @param defaultValue 校验失败时返回的默认值
     * @param <T>          返回值类型
     * @return 执行结果或默认值
     */
    private <T> T validateAndExecute(String key, Supplier<T> supplier, T defaultValue) {
        if (key == null || key.isEmpty()) {
            return defaultValue;
        }
        return supplier.get();
    }

    /**
     * 通用参数校验和执行方法（无返回值）
     *
     * @param key       需要校验的键
     * @param runnable  校验通过后执行的操作
     */
    private void validateAndExecute(String key, Runnable runnable) {
        if (key == null || key.isEmpty()) {
            return;
        }
        runnable.run();
    }

    /**
     * 关闭，释放资源
     */
    public void shutdown() {
        log.info("开始关闭RedisPvUvComponent...");
        try {
            // 最后一次同步
            syncToRedisNow();
            // 关闭线程池
            scheduledExecutorService.shutdown();
            // 等待任务完成
            if (!scheduledExecutorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                log.warn("线程池未在{}秒内正常关闭，强制关闭", SHUTDOWN_TIMEOUT_SECONDS);
                scheduledExecutorService.shutdownNow();
            }
            log.info("RedisPvUvComponent关闭完成");
        } catch (InterruptedException e) {
            log.warn("关闭过程被中断", e);
            scheduledExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("关闭RedisPvUvComponent时发生异常", e);
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