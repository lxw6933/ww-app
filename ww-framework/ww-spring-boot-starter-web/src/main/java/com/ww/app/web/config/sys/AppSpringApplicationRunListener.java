package com.ww.app.web.config.sys;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.*;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 应用启动过程监控器
 * 完整记录Spring应用启动各阶段耗时，识别启动瓶颈
 */
@Slf4j
@Component
public class AppSpringApplicationRunListener implements ApplicationListener<SpringApplicationEvent> {

    /**
     * 是否打印详细的启动时间信息
     */
    @Value("${app.startup.detailed-log:false}")
    private boolean detailedLog;

    /**
     * 警告阈值：超过此时间(ms)的启动阶段将以警告级别记录
     */
    @Value("${app.startup.warning-threshold:2000}")
    private long warningThreshold;

    /**
     * 启动各阶段的时间记录
     */
    private final Map<String, Long> startTimeMap = new LinkedHashMap<>();
    
    /**
     * 总启动开始时间
     */
    private Long applicationStartTime;

    /**
     * 处理Spring应用事件
     */
    @Override
    public void onApplicationEvent(@NotNull SpringApplicationEvent event) {
        if (event instanceof ApplicationStartingEvent) {
            // 应用开始启动
            applicationStartTime = System.currentTimeMillis();
            recordStage("应用启动开始");
        } else if (event instanceof ApplicationEnvironmentPreparedEvent) {
            // 环境准备完成
            recordStage("环境准备完成");
            logEnvironmentInfo(((ApplicationEnvironmentPreparedEvent) event).getEnvironment());
        } else if (event instanceof ApplicationContextInitializedEvent) {
            // 应用上下文初始化完成
            recordStage("应用上下文初始化完成");
        } else if (event instanceof ApplicationPreparedEvent) {
            // Bean定义加载完成
            recordStage("Bean定义加载完成");
        } else if (event instanceof ApplicationStartedEvent) {
            // 应用上下文刷新完成，此时所有Bean都已创建
            recordStage("应用上下文刷新完成");
        } else if (event instanceof ApplicationReadyEvent) {
            // 应用完全启动并准备好接收请求
            recordStage("应用准备就绪");
            logTotalStartupTime();
            logBeanCount(((ApplicationReadyEvent) event).getApplicationContext());
        } else if (event instanceof ApplicationFailedEvent) {
            // 应用启动失败
            recordStage("应用启动失败");
            ApplicationFailedEvent failedEvent = (ApplicationFailedEvent) event;
            log.error("应用启动失败: {}", failedEvent.getException().getMessage(), failedEvent.getException());
        }
    }

    /**
     * 记录启动阶段
     */
    private void recordStage(String stage) {
        long currentTime = System.currentTimeMillis();
        startTimeMap.put(stage, currentTime);
        
        if (detailedLog) {
            // 如果不是第一个阶段，计算与上一阶段的时间差
            if (startTimeMap.size() > 1) {
                String[] stages = startTimeMap.keySet().toArray(new String[0]);
                String previousStage = stages[stages.length - 2];
                long previousTime = startTimeMap.get(previousStage);
                long timeCost = currentTime - previousTime;
                
                if (timeCost > warningThreshold) {
                    log.warn("阶段耗时过长 - {}: {} ms", stage, timeCost);
                } else {
                    log.info("阶段耗时 - {}: {} ms", stage, timeCost);
                }
            } else {
                log.info("阶段开始 - {}", stage);
            }
        }
    }

    /**
     * 记录总启动时间
     */
    private void logTotalStartupTime() {
        if (applicationStartTime != null) {
            long totalTime = System.currentTimeMillis() - applicationStartTime;
            log.info("应用启动完成 - 总耗时: {} ms", totalTime);
            
            if (detailedLog) {
                logStageBreakdown();
            }
        }
    }
    
    /**
     * 记录各阶段时间明细
     */
    private void logStageBreakdown() {
        log.info("===== 应用启动阶段耗时明细 =====");
        String[] stages = startTimeMap.keySet().toArray(new String[0]);
        
        for (int i = 0; i < stages.length - 1; i++) {
            String currentStage = stages[i];
            String nextStage = stages[i + 1];
            
            long currentTime = startTimeMap.get(currentStage);
            long nextTime = startTimeMap.get(nextStage);
            long duration = nextTime - currentTime;
            
            log.info("{} => {}: {} ms", currentStage, nextStage, duration);
        }
        
        // 计算从最后一个阶段到当前的耗时
        if (stages.length > 0) {
            String lastStage = stages[stages.length - 1];
            long lastTime = startTimeMap.get(lastStage);
            long currentTime = System.currentTimeMillis();
            long duration = currentTime - lastTime;
            
            log.info("{} => 当前: {} ms", lastStage, duration);
        }
        
        log.info("==================================");
    }
    
    /**
     * 记录环境信息
     */
    private void logEnvironmentInfo(ConfigurableEnvironment environment) {
        if (detailedLog) {
            String[] activeProfiles = environment.getActiveProfiles();
            log.info("活动的配置文件: {}", String.join(", ", activeProfiles.length > 0 ? activeProfiles : new String[]{"default"}));
            
            // 记录一些关键配置信息
            log.info("应用名称: {}", environment.getProperty("spring.application.name", "未设置"));
            log.info("服务端口: {}", environment.getProperty("server.port", "未设置"));
        }
    }
    
    /**
     * 记录Bean数量
     */
    private void logBeanCount(ConfigurableApplicationContext context) {
        int beanCount = context.getBeanDefinitionCount();
        log.info("Bean定义总数: {}", beanCount);
        
        if (detailedLog) {
            log.info("容器中的Bean定义名称: {}", String.join(", ", context.getBeanDefinitionNames()));
        }
    }
}
