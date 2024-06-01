package com.ww.mall.common.utils;

import com.ww.mall.common.thread.DefaultThreadFactoryBuilder;
import com.ww.mall.common.thread.ThreadPoolExecutorMdcWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * @author ww
 * @create 2024-04-22- 11:07
 * @description:
 */
@Slf4j
public class MallThreadUtil {

    private MallThreadUtil() {}

    public static ThreadPoolExecutor initThreadPoolExecutor(String threadName,
                                                     Integer coreSize,
                                                     Integer maxSize,
                                                     Integer keepAliveTime,
                                                     TimeUnit timeUnit,
                                                     Integer queueLength,
                                                     RejectedExecutionHandler handler) {
        ThreadFactory threadFactory = new DefaultThreadFactoryBuilder().setNamePrefix(threadName).build();
        return new ThreadPoolExecutorMdcWrapper(coreSize,
                maxSize,
                keepAliveTime,
                timeUnit,
                new ArrayBlockingQueue<>(queueLength),
                threadFactory,
                handler
        );
    }

    public static ThreadPoolExecutor initFixedThreadPoolExecutor(String threadName, int threadSize) {
        ThreadFactory threadFactory = new DefaultThreadFactoryBuilder().setNamePrefix(threadName).build();
        return new ThreadPoolExecutorMdcWrapper(threadSize,
                threadSize,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory
        );
    }

}
