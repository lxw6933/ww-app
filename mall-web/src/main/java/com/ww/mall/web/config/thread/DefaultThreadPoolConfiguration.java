package com.ww.mall.web.config.thread;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * @description: 所有服务通用线程池配置
 * @author: ww
 * @create: 2023/7/16 18:22
 **/
@Slf4j
@Configuration
@EnableConfigurationProperties(DefaultThreadPoolProperties.class)
public class DefaultThreadPoolConfiguration {

    /**
     * 任务线程池
     * corePoolSize：核心池的大小
     * maximumPoolSize：线程池最大线程数
     * keepAliveTime：表示线程没有任务执行时最多保持多久时间会终止。
     * unit：参数keepAliveTime的时间单位
     * TimeUnit.DAYS：天
     * TimeUnit.HOURS：小时
     * TimeUnit.MINUTES：分钟
     * TimeUnit.SECONDS：秒
     * TimeUnit.MILLISECONDS：毫秒
     * TimeUnit.MICROSECONDS：微妙
     * TimeUnit.NANOSECONDS：纳秒
     * workQueue：一个阻塞队列，用来存储等待执行的任务
     * ArrayBlockingQueue：基于数组的先进先出队列，此队列创建时必须指定大小
     * PriorityBlockingQueue：优先级队列，它在PriorityQueue的基础上提供了可阻塞的读取操作。它是无限制的，就是说向Queue里面增加元素可能会失败（导致OurOfMemoryError）。
     * LinkedBlockingQueue：基于链表的先进先出队列，如果创建时没有指定此队列大小，则默认为Integer.MAX_VALUE，线程安全
     * SynchronousQueue：无界的，是一种无缓冲的等待队列，不会保存提交的任务，而是将直接新建一个线程来执行新来的任务。
     * threadFactory：线程工厂，主要用来创建线程
     * handler：表示当拒绝处理任务时的策略，有以下四种取值
     * ThreadPoolExecutor.AbortPolicy：丢弃任务并抛出RejectedExecutionException异常。
     * ThreadPoolExecutor.DiscardPolicy：也是丢弃任务，但是不抛出异常。
     * ThreadPoolExecutor.DiscardOldestPolicy：丢弃队列最前面的任务，然后重新尝试执行任务（重复此过程）
     * ThreadPoolExecutor.CallerRunsPolicy：由调用线程处理该任务
     *
     * @return ThreadPoolExecutor
     */
    @Bean(name = "defaultThreadPoolExecutor")
    public ThreadPoolExecutor defaultThreadPoolExecutor(DefaultThreadPoolProperties defaultThreadPoolProperties) {
        ThreadFactory threadFactory = new DefaultThreadFactoryBuilder().setNamePrefix(defaultThreadPoolProperties.getThreadName()).build();
        ThreadPoolExecutorMdcWrapper executor =
                new ThreadPoolExecutorMdcWrapper(defaultThreadPoolProperties.getCoreSize(),
                        defaultThreadPoolProperties.getMaxSize(),
                        defaultThreadPoolProperties.getKeepAliveTime(),
                        TimeUnit.SECONDS,
                        new ArrayBlockingQueue<>(defaultThreadPoolProperties.getQueueLength()),
                        threadFactory,
                        new ThreadPoolExecutor.CallerRunsPolicy()
                );
        log.info("初始化线程池DefaultThreadPoolExecutor成功...");
        return executor;
    }
}

