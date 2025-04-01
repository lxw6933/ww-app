package com.ww.app.common.utils;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author ww
 * @create 2023-07-15- 14:48
 * @description: ID生成工具类
 */
@Slf4j
public class IdUtil {
    
    private static final int MAX_WORKER_ID = 31;
    private static final int MAX_DATACENTER_ID = 31;
    private static final int DEFAULT_THREAD_POOL_SIZE = 200;
    private static final int DEFAULT_BATCH_SIZE = 100000;

    @Getter
    private static final long workerId;
    @Getter
    private static final long dataCenterId;
    private static final Snowflake snowflake;
    
    static {
        long tempWorkerId;
        long tempDataCenterId;
        Snowflake tempSnowflake;
        
        try {
            // 基于IP地址生成workerId,确保分布式环境下的唯一性
            tempWorkerId = NetUtil.ipv4ToLong(NetUtil.getLocalhostStr()) % (MAX_WORKER_ID + 1);
            // 随机生成数据中心ID
            tempDataCenterId = RandomUtil.randomInt(0, MAX_DATACENTER_ID);
            log.info("初始化雪花算法参数 - workerId: {}, dataCenterId: {}", tempWorkerId, tempDataCenterId);
            
            // 初始化雪花算法实例
            tempSnowflake = Singleton.get(Snowflake.class, tempWorkerId, tempDataCenterId, true);
        } catch (Exception e) {
            log.error("初始化雪花算法参数失败: {}", e.getMessage());
            // 发生异常时使用随机值
            tempWorkerId = RandomUtil.randomInt(0, MAX_WORKER_ID);
            tempDataCenterId = RandomUtil.randomInt(0, MAX_DATACENTER_ID);
            tempSnowflake = Singleton.get(Snowflake.class, tempWorkerId, tempDataCenterId, true);
            log.info("异常后重新初始化雪花算法参数 - workerId: {}, dataCenterId: {}", tempWorkerId, tempDataCenterId);
        }
        
        workerId = tempWorkerId;
        dataCenterId = tempDataCenterId;
        snowflake = tempSnowflake;
    }

    private IdUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 生成下一个ID
     *
     * @return 生成的ID
     */
    public static long nextId() {
        return snowflake.nextId();
    }

    /**
     * 生成下一个ID(字符串形式)
     *
     * @return 生成的ID字符串
     */
    public static String nextIdStr() {
        return snowflake.nextIdStr();
    }

    /**
     * 批量生成ID并验证唯一性
     *
     * @param batchSize 批量大小
     * @param threadPoolSize 线程池大小
     * @return 是否全部生成成功
     */
    public static boolean generateBatchIds(int batchSize, int threadPoolSize) {
        Set<String> container = new ConcurrentHashSet<>(batchSize);
        ExecutorService executorService = new ThreadPoolExecutor(
                threadPoolSize,
                threadPoolSize,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(batchSize),
                new ThreadFactory() {
                    private final AtomicLong counter = new AtomicLong(1);
                    @Override
                    public Thread newThread(@NonNull Runnable r) {
                        Thread thread = new Thread(r, "id-generator-" + counter.getAndIncrement());
                        thread.setDaemon(true);
                        return thread;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        try {
            // 提交任务
            for (int i = 0; i < batchSize; i++) {
                executorService.submit(() -> container.add(nextIdStr()));
            }
            
            // 等待所有任务完成
            executorService.shutdown();
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.error("线程池关闭超时");
                    return false;
                }
            }
            
            // 验证生成的ID数量是否正确
            return container.size() == batchSize;
        } catch (InterruptedException e) {
            log.error("批量生成ID被中断", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 使用默认参数批量生成ID
     *
     * @return 是否全部生成成功
     */
    public static boolean generateBatchIds() {
        return generateBatchIds(DEFAULT_BATCH_SIZE, DEFAULT_THREAD_POOL_SIZE);
    }

    /**
     * 主方法用于测试
     */
    public static void main(String[] args) {
        try {
            boolean success = generateBatchIds();
            log.info("批量生成ID测试结果: {}", success ? "成功" : "失败");
        } catch (Exception e) {
            log.error("批量生成ID测试异常", e);
        }
    }
}
