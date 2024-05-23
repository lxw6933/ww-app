package com.ww.mall.web.listener;

import com.alibaba.cloud.nacos.registry.NacosServiceRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-05-23- 15:53
 * @description: 确保服务端即时剔除重启的客户端，避免流量流入
 */
@Slf4j
@Component
public class NacosShutdownListener implements DisposableBean {

    @Resource
    private NacosServiceRegistry nacosServiceRegistry;

    @Resource
    private Registration registration;

    @Override
    public void destroy() throws Exception {
        // 主动调用 Nacos 服务下线操作
        if (nacosServiceRegistry != null && registration != null) {
            nacosServiceRegistry.deregister(registration);
            log.info("nacos service deregistered: {}", registration.getServiceId());
        }
    }
}

