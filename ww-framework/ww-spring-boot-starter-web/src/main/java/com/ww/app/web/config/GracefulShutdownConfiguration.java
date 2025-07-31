package com.ww.app.web.config;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.registry.NacosAutoServiceRegistration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

    private volatile boolean isRunning = false;

    @Override
    public void start() {
        isRunning = true;
        // 初始化关闭执行器
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
        } catch (Exception e) {
            log.error("优雅停机过程中发生异常", e);
        } finally {
            isRunning = false;
        }
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
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> nacosAutoServiceRegistration.stop());
            
            future.get(gracePeriodMs, TimeUnit.SECONDS);
            
            log.info("成功关闭nacos服务[{}][{}]【{}:{}】注册", 
                    nacosDiscoveryProperties.getService(), 
                    nacosDiscoveryProperties.getGroup(), 
                    nacosDiscoveryProperties.getIp(), 
                    nacosDiscoveryProperties.getPort());
        } catch (Exception e) {
            log.warn("注销Nacos服务实例过程中发生异常", e);
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
