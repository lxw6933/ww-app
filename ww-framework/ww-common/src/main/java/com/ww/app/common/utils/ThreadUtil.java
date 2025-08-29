package com.ww.app.common.utils;

import com.alibaba.ttl.threadpool.TtlExecutors;
import com.ww.app.common.thread.DefaultThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;

import static com.ww.app.common.constant.Constant.SHUTDOWN_TIMEOUT_SECONDS;

/**
 * @author ww
 * @create 2024-04-22- 11:07
 * @description: 使用skywalking bootstrap-plugin解决线程池异步任务丢失traceId
 */
@Slf4j
public class ThreadUtil {

    private ThreadUtil() {}

    public static ScheduledExecutorService initScheduledExecutorService(int threadSize) {
        return initScheduledExecutorService("app-scheduled-pool", threadSize);
    }

    public static ScheduledExecutorService initScheduledExecutorService(String threadName, int threadSize) {
        ThreadFactory threadFactory = new DefaultThreadFactoryBuilder().setNamePrefix(threadName).build();
        ScheduledExecutorService scheduledExecutorService;
        if (threadSize == 1) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
        } else {
            scheduledExecutorService = Executors.newScheduledThreadPool(threadSize, threadFactory);
        }
        return TtlExecutors.getTtlScheduledExecutorService(scheduledExecutorService);
    }

    public static ExecutorService initThreadPoolExecutor(String threadName,
                                                     Integer coreSize,
                                                     Integer maxSize,
                                                     Integer keepAliveTime,
                                                     TimeUnit timeUnit,
                                                     Integer queueLength,
                                                     RejectedExecutionHandler handler) {
        ThreadFactory threadFactory = new DefaultThreadFactoryBuilder().setNamePrefix(threadName).build();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(coreSize,
                maxSize,
                keepAliveTime,
                timeUnit,
                new ArrayBlockingQueue<>(queueLength),
                threadFactory,
                handler
        );
        return TtlExecutors.getTtlExecutorService(threadPoolExecutor);
    }

    public static ExecutorService initFixedThreadPoolExecutor(String threadName, int threadSize) {
        ThreadFactory threadFactory = new DefaultThreadFactoryBuilder().setNamePrefix(threadName).build();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(threadSize,
                threadSize,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory
        );
        return TtlExecutors.getTtlExecutorService(threadPoolExecutor);
    }

    /**
     * 创建通用的ThreadPoolTaskExecutor
     *
     * @param threadNamePrefix 线程名前缀
     * @param coreSize 核心线程数
     * @param maxSize 最大线程数
     * @param keepAliveTime 线程空闲时间
     * @param timeUnit 时间单位
     * @param queueCapacity 队列容量
     * @return ThreadPoolTaskExecutor
     */
    public static ThreadPoolTaskExecutor initThreadPoolTaskExecutor(String threadNamePrefix,
                                                                    Integer coreSize,
                                                                    Integer maxSize,
                                                                    Integer keepAliveTime,
                                                                    TimeUnit timeUnit,
                                                                    Integer queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(coreSize);
        // 最大线程数
        executor.setMaxPoolSize(maxSize);
        // 队列容量
        executor.setQueueCapacity(queueCapacity);
        // 线程名前缀
        executor.setThreadNamePrefix(threadNamePrefix);
        // 线程空闲时间
        executor.setKeepAliveSeconds((int) timeUnit.toSeconds(keepAliveTime));
        // 拒绝策略：由调用线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间
        executor.setAwaitTerminationSeconds(60);
        // 初始化
        executor.initialize();
        return executor;
    }

    /**
     * 创建通用的ThreadPoolTaskExecutor（简化版本）
     *
     * @param threadNamePrefix 线程名前缀
     * @param coreSize 核心线程数
     * @param maxSize 最大线程数
     * @param queueCapacity 队列容量
     * @return ThreadPoolTaskExecutor
     */
    public static ThreadPoolTaskExecutor initThreadPoolTaskExecutor(String threadNamePrefix,
                                                                    Integer coreSize,
                                                                    Integer maxSize,
                                                                    Integer queueCapacity) {
        return initThreadPoolTaskExecutor(threadNamePrefix, coreSize, maxSize, 60, TimeUnit.SECONDS, queueCapacity);
    }

    public static void shutdown(String name, Runnable task, ExecutorService executorService) {
        try {
            // 执行最后一次同步
            task.run();
            // 关闭线程池
            executorService.shutdown();
            // 等待任务完成
            if (!executorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                log.warn("{}线程池未在{}秒内正常关闭，强制关闭", name, SHUTDOWN_TIMEOUT_SECONDS);
                executorService.shutdownNow();
            }
            log.info("{}关闭完成", name);
        } catch (InterruptedException e) {
            log.warn("{}关闭过程被中断", name, e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("关闭{}时发生异常", name, e);
        }
    }

}
