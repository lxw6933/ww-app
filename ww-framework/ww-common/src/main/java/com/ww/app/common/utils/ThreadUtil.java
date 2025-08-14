package com.ww.app.common.utils;

import com.alibaba.ttl.threadpool.TtlExecutors;
import com.ww.app.common.thread.DefaultThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

import static com.ww.app.common.constant.Constant.SHUTDOWN_TIMEOUT_SECONDS;

/**
 * @author ww
 * @create 2024-04-22- 11:07
 * @description:
 */
@Slf4j
public class ThreadUtil {

    private ThreadUtil() {}

    public static ScheduledExecutorService initScheduledExecutorService(int threadSize) {
        ScheduledExecutorService scheduledExecutorService;
        if (threadSize == 1) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        } else {
            scheduledExecutorService = Executors.newScheduledThreadPool(threadSize);
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
