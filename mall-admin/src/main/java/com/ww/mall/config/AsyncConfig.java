package com.ww.mall.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.annotation.Resource;
import java.util.concurrent.Executor;

/**
 * @description: 异步执行配置
 * @author: ww
 * @create: 2021-06-03 10:47
 */
@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Resource
    private Executor threadPoolExecutor;

    @Override
    public Executor getAsyncExecutor() {
        return this.threadPoolExecutor;
    }

    /**
     * 处理异步执行任务异常
     * @return AsyncUncaughtExceptionHandler
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }

}
