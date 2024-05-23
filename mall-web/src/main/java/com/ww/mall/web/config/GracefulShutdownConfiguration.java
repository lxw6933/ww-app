package com.ww.mall.web.config;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import lombok.extern.slf4j.Slf4j;
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
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    @Resource
    private NamingService namingService;

    private boolean isRunning = false;

    @Override
    public void start() {
        isRunning = true;
    }

    @Override
    public void stop() {
        // 取消nacos服务实例的注册
        deregisterNacosInstance();
        isRunning = false;
    }

    private void deregisterNacosInstance() {
        if (namingService != null && nacosDiscoveryProperties != null) {
            try {
                log.info("即将关闭nacos服务【{}】【{}】【{}:{}】注册...", nacosDiscoveryProperties.getService(), nacosDiscoveryProperties.getGroup(), nacosDiscoveryProperties.getIp(), nacosDiscoveryProperties.getPort());
                namingService.deregisterInstance(
                        nacosDiscoveryProperties.getService(),
                        nacosDiscoveryProperties.getGroup(),
                        nacosDiscoveryProperties.getIp(),
                        nacosDiscoveryProperties.getPort());
                log.info("成功关闭nacos服务【{}】【{}】【{}:{}】注册...", nacosDiscoveryProperties.getService(), nacosDiscoveryProperties.getGroup(), nacosDiscoveryProperties.getIp(), nacosDiscoveryProperties.getPort());
            } catch (NacosException e) {
                log.error("关闭nacos服务【{}】【{}】【{}:{}】注册...异常：【{}】", nacosDiscoveryProperties.getService(), nacosDiscoveryProperties.getGroup(), nacosDiscoveryProperties.getIp(), nacosDiscoveryProperties.getPort(), e.getErrMsg());
            }
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public int getPhase() {
        // 确保此bean最后执行逻辑
        return Integer.MAX_VALUE;
    }
}
