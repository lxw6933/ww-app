package com.ww.app.common.utils;

import com.alibaba.ttl.threadpool.TtlExecutors;
import com.ww.app.common.thread.DefaultThreadFactoryBuilder;
import com.ww.app.common.thread.ThreadPoolExecutorMdcWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

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
        ThreadPoolExecutorMdcWrapper threadPoolExecutorMdcWrapper = new ThreadPoolExecutorMdcWrapper(coreSize,
                maxSize,
                keepAliveTime,
                timeUnit,
                new ArrayBlockingQueue<>(queueLength),
                threadFactory,
                handler
        );
        return TtlExecutors.getTtlExecutorService(threadPoolExecutorMdcWrapper);
    }

    public static ExecutorService initFixedThreadPoolExecutor(String threadName, int threadSize) {
        ThreadFactory threadFactory = new DefaultThreadFactoryBuilder().setNamePrefix(threadName).build();
        ThreadPoolExecutorMdcWrapper threadPoolExecutorMdcWrapper = new ThreadPoolExecutorMdcWrapper(threadSize,
                threadSize,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory
        );
        return TtlExecutors.getTtlExecutorService(threadPoolExecutorMdcWrapper);
    }

}
