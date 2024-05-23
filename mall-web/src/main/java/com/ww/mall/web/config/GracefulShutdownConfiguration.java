package com.ww.mall.web.config;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.registry.NacosAutoServiceRegistration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-05-23 20:06
 * @description: 优雅停机业务处理
 */
@Slf4j
@Component
public class GracefulShutdownConfiguration implements SmartLifecycle {

    @Resource
    private NacosAutoServiceRegistration nacosAutoServiceRegistration;

    @Resource
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    private boolean isRunning = false;

    @Override
    public void start() {
        isRunning = true;
    }

    @Override
    public void stop() {
        // 取消nacos服务实例的注册
        deregisterNacosInstance();
        // 关闭容器
        SpringApplication.exit(SpringUtil.getApplicationContext());
        ((ConfigurableApplicationContext) SpringUtil.getApplicationContext()).close();
        isRunning = false;
    }

    private void deregisterNacosInstance() {
        log.info("即将关闭nacos服务【{}】【{}】【{}:{}】注册...", nacosDiscoveryProperties.getService(), nacosDiscoveryProperties.getGroup(), nacosDiscoveryProperties.getIp(), nacosDiscoveryProperties.getPort());
        nacosAutoServiceRegistration.stop();
        log.info("成功关闭nacos服务【{}】【{}】【{}:{}】注册...", nacosDiscoveryProperties.getService(), nacosDiscoveryProperties.getGroup(), nacosDiscoveryProperties.getIp(), nacosDiscoveryProperties.getPort());
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
