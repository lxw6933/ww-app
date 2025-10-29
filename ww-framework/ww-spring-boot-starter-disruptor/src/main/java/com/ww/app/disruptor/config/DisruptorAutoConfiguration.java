package com.ww.app.disruptor.config;

import com.ww.app.disruptor.model.ProcessResult;
import com.ww.app.disruptor.api.DisruptorTemplate;
import com.ww.app.disruptor.core.DisruptorConfig;
import com.ww.app.disruptor.core.DisruptorEngine;
import com.ww.app.disruptor.processor.BatchEventProcessor;
import com.ww.app.disruptor.processor.EventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.util.List;

/**
 * Disruptor自动配置类
 *
 * @author ww-framework
 */
@Configuration
@EnableConfigurationProperties(DisruptorProperties.class)
@ConditionalOnProperty(prefix = "ww.disruptor", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DisruptorAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DisruptorAutoConfiguration.class);

    private DisruptorTemplate<?> disruptorTemplate;

    /**
     * 创建Disruptor配置
     */
    @Bean
    @ConditionalOnMissingBean
    public DisruptorConfig disruptorConfig(DisruptorProperties properties) {
        log.info("初始化Disruptor配置");

        DisruptorConfig config = new DisruptorConfig();
        config.setRingBufferSize(properties.getRingBufferSize());
        config.setConsumerThreads(properties.getConsumerThreads());
        config.setProducerThreads(properties.getProducerThreads());
        config.setBatchSize(properties.getBatchSize());
        config.setBatchTimeout(properties.getBatchTimeout());
        config.setWaitStrategy(properties.getWaitStrategy());
        config.setBatchEnabled(properties.isBatchEnabled());

        // 验证配置
        config.validate();

        log.info("Disruptor配置: {}", config);
        return config;
    }

    /**
     * 创建Disruptor引擎
     */
    @Bean
    @ConditionalOnMissingBean
    public DisruptorEngine<Object> disruptorEngine(
            DisruptorConfig config,
            List<EventProcessor<Object>> eventProcessors,
            List<BatchEventProcessor<Object>> batchEventProcessors) {

        log.info("初始化Disruptor引擎");

        DisruptorEngine<Object> engine = new DisruptorEngine<>(config);

        // 设置批量处理器（优先）
        if (!batchEventProcessors.isEmpty()) {
            BatchEventProcessor<Object> processor = batchEventProcessors.get(0);
            engine.setBatchEventProcessor(processor);
            log.info("使用批量事件处理器: {}", processor.getName());
        }
        // 设置单个事件处理器
        else if (!eventProcessors.isEmpty()) {
            EventProcessor<Object> processor = eventProcessors.get(0);
            engine.setEventProcessor(processor);
            log.info("使用事件处理器: {}", processor.getName());
        } else {
            log.warn("未找到任何事件处理器，使用默认处理器");
            // 设置默认处理器
            engine.setEventProcessor(event -> {
                log.debug("默认处理器处理事件: {}", event.getEventId());
                return ProcessResult.success();
            });
        }

        // 启动引擎
        engine.start();

        log.info("Disruptor引擎启动成功");
        return engine;
    }

    /**
     * 创建DisruptorTemplate
     */
    @Bean
    @ConditionalOnMissingBean
    public DisruptorTemplate<Object> disruptorTemplate(DisruptorEngine<Object> engine) {
        log.info("创建DisruptorTemplate");

        DisruptorTemplate<Object> template = DisruptorTemplate.builder()
                .ringBufferSize(engine.getPendingCount() > 0 ? (int) engine.getPendingCount() : 1024)
                .eventProcessor(event -> {
                    log.debug("模板处理器处理事件: {}", event.getEventId());
                    return ProcessResult.success();
                })
                .build();

        this.disruptorTemplate = template;
        return template;
    }

    /**
     * 应用停止时关闭引擎
     */
    @PreDestroy
    public void destroy() {
        log.info("正在关闭Disruptor引擎...");
        if (disruptorTemplate != null && disruptorTemplate.isRunning()) {
            disruptorTemplate.stop();
        }
        log.info("Disruptor引擎已关闭");
    }
}
