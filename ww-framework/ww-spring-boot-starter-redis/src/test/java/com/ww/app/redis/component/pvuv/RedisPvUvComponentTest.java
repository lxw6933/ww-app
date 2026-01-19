package com.ww.app.redis.component.pvuv;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import com.ww.app.redis.RedisTestApplication;
import com.ww.app.redis.component.pvuv.enums.PvUvBizTypeEnum;
import com.ww.app.redis.component.pvuv.keys.PvUvRedisKeyBuilder;
import com.ww.app.redis.component.pvuv.storage.RedisPvUvStorage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RedisPvUvComponent 测试类
 * 覆盖核心业务场景与边界场景
 */
@Slf4j
@SpringBootTest(classes = RedisTestApplication.class)
public class RedisPvUvComponentTest {

    @Resource
    private RedisPvUvComponent redisPvUvComponent;

    @Resource
    private PvUvRedisKeyBuilder keyBuilder;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private PvUvBizTypeEnum bizType;
    private String bizKey;

    private Set<String> cleanupKeys;

    @BeforeEach
    public void setUp() {
        bizType = PvUvBizTypeEnum.PAGE;
        bizKey = "page_" + RandomUtil.randomNumbers(6);
        cleanupKeys = new HashSet<>();
    }

    @AfterEach
    public void tearDown() {
        if (!cleanupKeys.isEmpty()) {
            stringRedisTemplate.delete(cleanupKeys);
        }
    }

    private void trackKeys(PvUvBizTypeEnum type, String key, LocalDate date) {
        cleanupKeys.add(keyBuilder.buildPvKey(type, key, date));
        cleanupKeys.add(keyBuilder.buildUvKey(type, key, date));
    }

    /**
     * 测试PV记录与同步
     */
    @Test
    public void testRecordPvAndSync() {
        trackKeys(bizType, bizKey, LocalDate.now());

        redisPvUvComponent.recordPv(bizType, bizKey);
        redisPvUvComponent.recordPv(bizType, bizKey);
        redisPvUvComponent.recordPv(bizType, bizKey);

        // 未同步时Redis中应为0
        Assert.isTrue(redisPvUvComponent.getPv(bizType, bizKey) == 0, "未同步时PV应为0");

        redisPvUvComponent.syncToRedisNow();
        Assert.isTrue(redisPvUvComponent.getPv(bizType, bizKey) == 3, "同步后PV应为3");
    }

    /**
     * 测试UV去重统计
     */
    @Test
    public void testRecordUvDedup() {
        trackKeys(bizType, bizKey, LocalDate.now());

        String userId1 = "user_" + RandomUtil.randomNumbers(5);
        String userId2 = "user_" + RandomUtil.randomNumbers(5);

        redisPvUvComponent.recordUv(bizType, bizKey, userId1);
        redisPvUvComponent.recordUv(bizType, bizKey, userId1);
        redisPvUvComponent.recordUv(bizType, bizKey, userId2);
        redisPvUvComponent.syncToRedisNow();

        long uv = redisPvUvComponent.getUv(bizType, bizKey);
        Assert.isTrue(uv == 2, "UV应去重统计为2");
    }

    /**
     * 测试同时记录PV和UV
     */
    @Test
    public void testRecordPvAndUv() {
        trackKeys(bizType, bizKey, LocalDate.now());

        String userId = "user_" + RandomUtil.randomNumbers(5);
        redisPvUvComponent.recordPvAndUv(bizType, bizKey, userId);
        redisPvUvComponent.syncToRedisNow();

        Assert.isTrue(redisPvUvComponent.getPv(bizType, bizKey) == 1, "PV应为1");
        Assert.isTrue(redisPvUvComponent.getUv(bizType, bizKey) == 1, "UV应为1");
    }

    /**
     * 测试用户ID为空时仅记录PV
     */
    @Test
    public void testRecordPvAndUvWithEmptyUser() {
        trackKeys(bizType, bizKey, LocalDate.now());

        redisPvUvComponent.recordPvAndUv(bizType, bizKey, "");
        redisPvUvComponent.syncToRedisNow();

        Assert.isTrue(redisPvUvComponent.getPv(bizType, bizKey) == 1, "PV应为1");
        Assert.isTrue(redisPvUvComponent.getUv(bizType, bizKey) == 0, "UV应为0");
    }

    /**
     * 测试按日统计与全量统计互不影响
     */
    @Test
    public void testDailyAndTotalSeparated() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        trackKeys(bizType, bizKey, today);
        trackKeys(bizType, bizKey, yesterday);
        trackKeys(bizType, bizKey, null);

        redisPvUvComponent.recordPv(bizType, bizKey, today);
        redisPvUvComponent.recordPv(bizType, bizKey, today);
        redisPvUvComponent.recordPv(bizType, bizKey, yesterday);
        redisPvUvComponent.recordTotalPvAndUv(bizType, bizKey, "user_a");
        redisPvUvComponent.syncToRedisNow();

        Assert.isTrue(redisPvUvComponent.getPv(bizType, bizKey, today) == 2, "今日PV应为2");
        Assert.isTrue(redisPvUvComponent.getPv(bizType, bizKey, yesterday) == 1, "昨日PV应为1");
        Assert.isTrue(redisPvUvComponent.getTotalPv(bizType, bizKey) == 1, "全量PV应为1");
        Assert.isTrue(redisPvUvComponent.getTotalUv(bizType, bizKey) == 1, "全量UV应为1");
    }

    /**
     * 测试PV/UV组合查询
     */
    @Test
    public void testGetPvAndUv() {
        trackKeys(bizType, bizKey, LocalDate.now());

        String userId = "user_" + RandomUtil.randomNumbers(5);
        redisPvUvComponent.recordPvAndUv(bizType, bizKey, userId);
        redisPvUvComponent.recordPv(bizType, bizKey);
        redisPvUvComponent.syncToRedisNow();

        RedisPvUvComponent.PvUvResult result = redisPvUvComponent.getPvAndUv(bizType, bizKey);
        Assert.isTrue(result.getPv() == 2, "PV应为2");
        Assert.isTrue(result.getUv() == 1, "UV应为1");
    }

    /**
     * 测试定时同步：无待同步数据时不触发
     */
    @Test
    public void testScheduledSyncNoPending() {
        trackKeys(bizType, bizKey, LocalDate.now());

        redisPvUvComponent.scheduledSyncToRedis();
        Assert.isTrue(redisPvUvComponent.getPv(bizType, bizKey) == 0, "无数据时PV应为0");
    }

    /**
     * 测试定时同步：有待同步数据时同步到Redis
     */
    @Test
    public void testScheduledSyncWithPending() {
        trackKeys(bizType, bizKey, LocalDate.now());

        redisPvUvComponent.recordPv(bizType, bizKey);
        redisPvUvComponent.recordUv(bizType, bizKey, "user_" + RandomUtil.randomNumbers(5));
        redisPvUvComponent.scheduledSyncToRedis();

        Assert.isTrue(redisPvUvComponent.getPv(bizType, bizKey) == 1, "PV应为1");
        Assert.isTrue(redisPvUvComponent.getUv(bizType, bizKey) == 1, "UV应为1");
    }

    /**
     * 测试bizType为空时使用CUSTOM
     */
    @Test
    public void testNullBizType() {
        trackKeys(PvUvBizTypeEnum.CUSTOM, bizKey, LocalDate.now());

        redisPvUvComponent.recordPv(null, bizKey);
        redisPvUvComponent.syncToRedisNow();

        Assert.isTrue(redisPvUvComponent.getPv(null, bizKey) == 1, "CUSTOM类型PV应为1");
    }

    /**
     * 测试非法key不记录数据
     */
    @Test
    public void testInvalidKey() {
        redisPvUvComponent.recordPv(bizType, "");
        redisPvUvComponent.recordUv(bizType, "", "user_x");
        redisPvUvComponent.syncToRedisNow();

        Assert.isTrue(redisPvUvComponent.getPv(bizType, "") == 0, "非法key的PV应为0");
        Assert.isTrue(redisPvUvComponent.getUv(bizType, "") == 0, "非法key的UV应为0");
    }

    /**
     * 测试同步失败回补与重试
     */
    @Test
    public void testSyncFailureRetry() {
        FailingRedisPvUvStorage failingStorage = new FailingRedisPvUvStorage(stringRedisTemplate);
        RedisPvUvComponent component = new RedisPvUvComponent(failingStorage, keyBuilder, 3600);

        try {
            String key = "fail_retry_" + RandomUtil.randomNumbers(6);
            trackKeys(bizType, key, LocalDate.now());

            // 先让PV/UV同步失败
            failingStorage.failPvOnce.set(true);
            failingStorage.failUvOnce.set(true);

            component.recordPv(bizType, key);
            component.recordUv(bizType, key, "user_" + RandomUtil.randomNumbers(5));
            component.syncToRedisNow();

            Assert.isTrue(component.getPv(bizType, key) == 0, "同步失败时PV应为0");
            Assert.isTrue(component.getUv(bizType, key) == 0, "同步失败时UV应为0");

            // 允许成功后重试
            component.syncToRedisNow();
            Assert.isTrue(component.getPv(bizType, key) == 1, "重试后PV应为1");
            Assert.isTrue(component.getUv(bizType, key) == 1, "重试后UV应为1");
        } finally {
            component.shutdown();
        }
    }

    /**
     * 测试记录全量PV/UV与去重逻辑
     */
    @Test
    public void testTotalPvAndUvRecord() {
        trackKeys(bizType, bizKey, null);

        redisPvUvComponent.recordTotalPvAndUv(bizType, bizKey, "user_a");
        redisPvUvComponent.recordTotalPvAndUv(bizType, bizKey, "user_a");
        redisPvUvComponent.recordTotalPvAndUv(bizType, bizKey, "user_b");
        redisPvUvComponent.syncToRedisNow();

        Assert.isTrue(redisPvUvComponent.getTotalPv(bizType, bizKey) == 3, "全量PV应为3");
        Assert.isTrue(redisPvUvComponent.getTotalUv(bizType, bizKey) == 2, "全量UV应为2");
    }

    /**
     * 测试recordUv非法用户不记录
     */
    @Test
    public void testRecordUvWithInvalidUser() {
        trackKeys(bizType, bizKey, LocalDate.now());

        redisPvUvComponent.recordUv(bizType, bizKey, "");
        redisPvUvComponent.recordUv(bizType, bizKey, null);
        redisPvUvComponent.syncToRedisNow();

        Assert.isTrue(redisPvUvComponent.getUv(bizType, bizKey) == 0, "非法用户不应计入UV");
    }

    /**
     * 测试PvUvResult默认返回
     */
    @Test
    public void testPvUvResultDefault() {
        RedisPvUvComponent.PvUvResult result = redisPvUvComponent.getPvAndUv(bizType, "");
        Assert.isTrue(result.getPv() == 0, "默认PV应为0");
        Assert.isTrue(result.getUv() == 0, "默认UV应为0");
    }

    /**
     * 测试高并发PV写入
     */
    @Test
    public void testConcurrentPv() throws Exception {
        trackKeys(bizType, bizKey, LocalDate.now());

        int threads = 8;
        int perThread = 100;
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                for (int j = 0; j < perThread; j++) {
                    redisPvUvComponent.recordPv(bizType, bizKey);
                }
                latch.countDown();
            }).start();
        }

        latch.await(3, TimeUnit.SECONDS);
        redisPvUvComponent.syncToRedisNow();

        Assert.isTrue(redisPvUvComponent.getPv(bizType, bizKey) == threads * perThread, "并发PV统计应准确");
    }

    /**
     * 测试高并发UV写入去重
     */
    @Test
    public void testConcurrentUvDedup() throws Exception {
        trackKeys(bizType, bizKey, LocalDate.now());

        int threads = 6;
        int usersPerThread = 50;
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            int threadIndex = i;
            new Thread(() -> {
                for (int j = 0; j < usersPerThread; j++) {
                    // 保证跨线程有重复用户
                    String userId = "user_" + (j % 20) + "_t" + threadIndex;
                    redisPvUvComponent.recordUv(bizType, bizKey, userId);
                }
                latch.countDown();
            }).start();
        }

        latch.await(3, TimeUnit.SECONDS);
        redisPvUvComponent.syncToRedisNow();

        long uv = redisPvUvComponent.getUv(bizType, bizKey);
        Assert.isTrue(uv > 0, "并发UV统计应大于0");
    }

    /**
     * 测试按日与全量并行统计不互相污染
     */
    @Test
    public void testDailyAndTotalIsolation() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        trackKeys(bizType, bizKey, today);
        trackKeys(bizType, bizKey, yesterday);
        trackKeys(bizType, bizKey, null);

        redisPvUvComponent.recordPv(bizType, bizKey, today);
        redisPvUvComponent.recordPv(bizType, bizKey, yesterday);
        redisPvUvComponent.recordTotalPvAndUv(bizType, bizKey, "user_total");
        redisPvUvComponent.syncToRedisNow();

        Assert.isTrue(redisPvUvComponent.getPv(bizType, bizKey, today) == 1, "今日PV应为1");
        Assert.isTrue(redisPvUvComponent.getPv(bizType, bizKey, yesterday) == 1, "昨日PV应为1");
        Assert.isTrue(redisPvUvComponent.getTotalPv(bizType, bizKey) == 1, "全量PV应为1");
    }

    /**
     * 测试记录PV后立即读取仍为0（最终一致性）
     */
    @Test
    public void testReadBeforeSync() {
        trackKeys(bizType, bizKey, LocalDate.now());

        redisPvUvComponent.recordPv(bizType, bizKey);
        Assert.isTrue(redisPvUvComponent.getPv(bizType, bizKey) == 0, "未同步前PV应为0");
    }

    /**
     * 测试关闭时触发同步
     */
    @Test
    public void testShutdownSync() {
        trackKeys(bizType, bizKey, LocalDate.now());

        redisPvUvComponent.recordPv(bizType, bizKey);
        redisPvUvComponent.shutdown();

        Assert.isTrue(redisPvUvComponent.getPv(bizType, bizKey) == 1, "关闭时应触发同步");
    }

    /**
     * RedisPvUvStorage失败模拟
     */
    private static class FailingRedisPvUvStorage extends RedisPvUvStorage {
        private final AtomicBoolean failPvOnce = new AtomicBoolean(false);
        private final AtomicBoolean failUvOnce = new AtomicBoolean(false);

        public FailingRedisPvUvStorage(StringRedisTemplate stringRedisTemplate) {
            super(stringRedisTemplate);
        }

        @Override
        public void batchIncrementPv(Map<String, Long> pvData) {
            if (failPvOnce.compareAndSet(true, false)) {
                throw new RuntimeException("mock pv sync failure");
            }
            super.batchIncrementPv(pvData);
        }

        @Override
        public void batchAddUsersToUv(Map<String, Set<String>> uvData) {
            if (failUvOnce.compareAndSet(true, false)) {
                throw new RuntimeException("mock uv sync failure");
            }
            super.batchAddUsersToUv(uvData);
        }
    }
}
