package com.ww.mall.monitor.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2025-01-02 22:35
 * @description: 监控配置类
 */
@Configuration
public class AppMonitorAutoConfiguration {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(@Value("${spring.application.name}") String applicationName) {
        // 解决grafana拉取prometheus数据无法获取到服务application name问题
        return registry -> registry.config().commonTags("application", applicationName);
    }

}
