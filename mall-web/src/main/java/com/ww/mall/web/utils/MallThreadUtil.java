package com.ww.mall.web.utils;

import com.ww.mall.web.config.thread.DefaultThreadFactoryBuilder;
import com.ww.mall.web.config.thread.ThreadPoolExecutorMdcWrapper;
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

    public ThreadPoolExecutor initThreadPoolExecutor(String threadName,
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

}
