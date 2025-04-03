package com.ww.app.web.config;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.registry.NacosAutoServiceRegistration;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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
     * 关闭执行器
     */
    private ExecutorService shutdownExecutor;

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
            // 取消nacos服务实例的注册，不再接收新请求
            deregisterNacosInstance();
            // 关闭应用上下文
            shutdownApplicationContext();
            // 关闭线程池
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
            public Thread newThread(@NonNull Runnable r) {
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
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> nacosAutoServiceRegistration.stop(), shutdownExecutor);
            
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
