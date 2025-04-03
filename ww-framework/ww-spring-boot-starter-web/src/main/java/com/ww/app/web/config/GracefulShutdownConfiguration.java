package com.ww.app.web.config;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.registry.NacosAutoServiceRegistration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author ww
 * @create 2024-05-23 20:06
 * @description: 优雅停机业务处理，支持高并发场景
 */
@Slf4j
@Component
public class GracefulShutdownConfiguration implements SmartLifecycle {

    @Resource
    private NacosAutoServiceRegistration nacosAutoServiceRegistration;

    @Resource
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    /**
     * 优雅停机超时时间（毫秒）
     */
    @Value("${server.shutdown.grace-period:30000}")
    private long gracePeriodMs;

    /**
     * 活跃请求等待超时时间（毫秒）
     */
    @Value("${server.shutdown.request-timeout:15000}")
    private long requestTimeoutMs;

    /**
     * 最大并发关闭线程数
     */
    @Value("${server.shutdown.max-threads:4}")
    private int maxShutdownThreads;

    private volatile boolean isRunning = false;
    
    /**
     * 当前正在处理的请求数量
     */
    private final AtomicInteger activeRequests = new AtomicInteger(0);
    
    /**
     * 请求计数器
     */
    private final AtomicLong requestCounter = new AtomicLong(0);
    
    /**
     * 关闭执行器
     */
    private ExecutorService shutdownExecutor;

    /**
     * 增加活跃请求计数
     */
    public void incrementActiveRequests() {
        activeRequests.incrementAndGet();
        requestCounter.incrementAndGet();
    }

    /**
     * 减少活跃请求计数
     */
    public void decrementActiveRequests() {
        activeRequests.decrementAndGet();
    }

    /**
     * 获取当前活跃请求数
     */
    public int getActiveRequestCount() {
        return activeRequests.get();
    }

    /**
     * 获取总请求数
     */
    public long getTotalRequestCount() {
        return requestCounter.get();
    }

    @Override
    public void start() {
        isRunning = true;
        // 初始化关闭执行器
        this.shutdownExecutor = createShutdownExecutor();
        log.info("应用服务已启动，优雅停机配置：超时时间={}ms，请求等待超时={}ms", gracePeriodMs, requestTimeoutMs);
    }

    @Override
    public void stop() {
        if (!isRunning) {
            return;
        }
        log.info("接收到停机信号，开始优雅停机流程...");
        try {
            // 1. 取消nacos服务实例的注册，不再接收新请求
            deregisterNacosInstance();
            // 2. 等待所有当前请求处理完成
            waitForActiveRequestsToComplete();
            // 3. 关闭应用上下文
            shutdownApplicationContext();
            // 4. 关闭线程池
            shutdownExecutor.shutdown();
            try {
                if (!shutdownExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    shutdownExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                shutdownExecutor.shutdownNow();
            }
            
        } catch (Exception e) {
            log.error("优雅停机过程中发生异常", e);
        } finally {
            isRunning = false;
            log.info("应用服务已完全停止");
        }
    }

    /**
     * 创建关闭执行器
     */
    private ExecutorService createShutdownExecutor() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("graceful-shutdown-" + counter.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        };
        
        return new ThreadPoolExecutor(
                1, 
                maxShutdownThreads,
                0L, 
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * 注销Nacos服务实例
     */
    private void deregisterNacosInstance() {
        try {
            log.info("即将关闭nacos服务[{}][{}]【{}:{}】注册...", 
                    nacosDiscoveryProperties.getService(), 
                    nacosDiscoveryProperties.getGroup(), 
                    nacosDiscoveryProperties.getIp(), 
                    nacosDiscoveryProperties.getPort());
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                nacosAutoServiceRegistration.stop();
            }, shutdownExecutor);
            
            future.get(10, TimeUnit.SECONDS);
            
            log.info("成功关闭nacos服务[{}][{}]【{}:{}】注册", 
                    nacosDiscoveryProperties.getService(), 
                    nacosDiscoveryProperties.getGroup(), 
                    nacosDiscoveryProperties.getIp(), 
                    nacosDiscoveryProperties.getPort());
        } catch (Exception e) {
            log.warn("注销Nacos服务实例过程中发生异常", e);
        }
    }

    /**
     * 等待活跃请求处理完成
     */
    private void waitForActiveRequestsToComplete() {
        final long startTime = System.currentTimeMillis();
        final int initialCount = activeRequests.get();
        if (initialCount <= 0) {
            log.info("当前无活跃请求，无需等待");
            return;
        }
        log.info("等待{}个活跃请求处理完成，最大等待时间为{}ms", initialCount, requestTimeoutMs);
        while (activeRequests.get() > 0) {
            try {
                // 每100ms检查一次
                Thread.sleep(100);
                long elapsedTime = System.currentTimeMillis() - startTime;
                if (elapsedTime > requestTimeoutMs) {
                    log.warn("等待活跃请求超时！仍有{}个请求未处理完成", activeRequests.get());
                    break;
                }
                // 每秒打印一次日志
                if (elapsedTime % 1000 < 100) {
                    log.info("正在等待{}个活跃请求处理完成，已等待{}ms", activeRequests.get(), elapsedTime);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("等待请求完成过程中被中断");
                break;
            }
        }
        log.info("所有活跃请求已处理完成，用时{}ms", System.currentTimeMillis() - startTime);
    }

    /**
     * 关闭应用上下文
     */
    private void shutdownApplicationContext() {
        try {
            log.info("开始关闭Spring应用上下文...");
            ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) SpringUtil.getApplicationContext();
            // 先退出，再关闭
            CompletableFuture<Integer> exitFuture = CompletableFuture.supplyAsync(() -> 
                SpringApplication.exit(applicationContext), shutdownExecutor);
            Integer exitCode = exitFuture.get(gracePeriodMs / 2, TimeUnit.MILLISECONDS);
            log.info("Spring应用退出完成，退出码: {}", exitCode);
            CompletableFuture<Void> closeFuture = CompletableFuture.runAsync(applicationContext::close, shutdownExecutor);
            closeFuture.get(gracePeriodMs / 2, TimeUnit.MILLISECONDS);
            log.info("Spring应用上下文已关闭");
        } catch (Exception e) {
            log.error("关闭Spring应用上下文过程中发生异常", e);
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
