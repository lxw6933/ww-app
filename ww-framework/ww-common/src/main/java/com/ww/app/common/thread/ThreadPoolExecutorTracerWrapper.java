package com.ww.app.common.thread;

import lombok.NonNull;
import org.apache.skywalking.apm.toolkit.trace.CallableWrapper;
import org.apache.skywalking.apm.toolkit.trace.RunnableWrapper;

import java.util.concurrent.*;

/**
 * @description: 子线程日志打印丢失 skywalking traceId
 * @author: ww
 * @create: 2023/7/8 11:29
 **/
public class ThreadPoolExecutorTracerWrapper extends ThreadPoolExecutor {

    public ThreadPoolExecutorTracerWrapper(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                           BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public ThreadPoolExecutorTracerWrapper(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                           BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public ThreadPoolExecutorTracerWrapper(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                           BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public ThreadPoolExecutorTracerWrapper(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                           BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
                                           RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    public void execute(@NonNull Runnable task) {
        super.execute(RunnableWrapper.of(task));
    }

    @NonNull
    @Override
    public <T> Future<T> submit(@NonNull Runnable task, T result) {
        return super.submit(RunnableWrapper.of(task), result);
    }

    @NonNull
    @Override
    public <T> Future<T> submit(@NonNull Callable<T> task) {
        return super.submit(CallableWrapper.of(task));
    }

    @NonNull
    @Override
    public Future<?> submit(@NonNull Runnable task) {
        return super.submit(RunnableWrapper.of(task));
    }
}
