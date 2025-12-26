package com.ww.app.open.config;

import com.ww.app.disruptor.api.DisruptorTemplate;
import com.ww.app.disruptor.constans.DisruptorWaitStrategy;
import com.ww.app.open.entity.OpenApiCallLog;
import com.ww.app.open.processor.OpenApiLogBatchProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * Disruptor 配置：用于开放平台日志/统计异步处理
 */
@Slf4j
@Configuration
public class OpenApiDisruptorConfig {

    @Resource
    private OpenApiLogBatchProcessor openApiLogBatchProcessor;

    private DisruptorTemplate<OpenApiCallLog> openApiLogDisruptorTemplate;

    @Bean
    public DisruptorTemplate<OpenApiCallLog> openApiLogDisruptorTemplate() {
        int consumerThreads = Math.max(4, Runtime.getRuntime().availableProcessors());
        openApiLogDisruptorTemplate = DisruptorTemplate.<OpenApiCallLog>builder()
                .businessName("open-api-log")
                .ringBufferSize(8192)
                .consumerThreads(consumerThreads)
                .waitStrategy(DisruptorWaitStrategy.BLOCKING)
                .batchEnabled(true)
                .batchSize(200)
                .batchTimeout(200)
                .batchEventProcessor(openApiLogBatchProcessor)
                .build();

        openApiLogDisruptorTemplate.start();
        log.info("OpenApi Disruptor 启动完成: consumerThreads={}", consumerThreads);
        return openApiLogDisruptorTemplate;
    }

    @PreDestroy
    public void destroy() {
        if (openApiLogDisruptorTemplate != null && openApiLogDisruptorTemplate.isRunning()) {
            log.info("关闭 OpenApi Disruptor...");
            openApiLogDisruptorTemplate.stop();
        }
    }
}


