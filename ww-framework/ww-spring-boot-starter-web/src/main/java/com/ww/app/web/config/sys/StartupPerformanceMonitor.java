package com.ww.app.web.config.sys;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 应用启动性能监控配置
 * 提供应用启动过程和Bean初始化的性能监控功能
 */
@Slf4j
@Configuration
public class StartupPerformanceMonitor {

    /**
     * 启动监控配置属性类
     */
    @Bean
    @ConfigurationProperties(prefix = "app.startup.monitor")
    public StartupMonitorProperties startupMonitorProperties() {
        return new StartupMonitorProperties();
    }

    /**
     * JVM信息收集器
     * 用于收集和记录JVM相关信息，帮助分析性能问题
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.startup.monitor", name = "jvm-info-enabled", havingValue = "true", matchIfMissing = true)
    public JvmInfoCollector jvmInfoCollector() {
        return new JvmInfoCollector();
    }

    /**
     * 系统资源监控器
     * 在应用启动完成后监控系统资源使用情况
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.startup.monitor", name = "system-resources-enabled", havingValue = "false", matchIfMissing = true)
    public SystemResourcesMonitor systemResourcesMonitor() {
        return new SystemResourcesMonitor();
    }

    /**
     * 启动监控配置属性
     */
    @Getter
    @Setter
    public static class StartupMonitorProperties {
        /**
         * 是否启用JVM信息收集
         */
        private boolean jvmInfoEnabled = true;

        /**
         * 是否启用系统资源监控
         */
        private boolean systemResourcesEnabled = true;

        /**
         * 是否启用详细日志
         */
        private boolean detailedLog = false;

        /**
         * 是否在应用启动后自动生成报告
         */
        private boolean reportEnabled = true;

        /**
         * 性能警告阈值(毫秒)
         */
        private long warningThreshold = 500;
    }
} 