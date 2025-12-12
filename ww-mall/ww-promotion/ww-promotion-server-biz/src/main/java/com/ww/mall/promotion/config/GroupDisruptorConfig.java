package com.ww.mall.promotion.config;

import com.ww.app.disruptor.api.DisruptorTemplate;
import com.ww.app.disruptor.constans.DisruptorWaitStrategy;
import com.ww.mall.promotion.event.GroupEvent;
import com.ww.mall.promotion.processor.GroupEventProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * 拼团Disruptor配置
 * 
 * @author ww
 * @create 2025-12-08
 * @description: 配置拼团异步事件处理的Disruptor
 */
@Slf4j
@Configuration
public class GroupDisruptorConfig {

    @Resource
    private GroupEventProcessor groupEventProcessor;

    private DisruptorTemplate<GroupEvent> groupDisruptorTemplate;

    @Bean
    public DisruptorTemplate<GroupEvent> groupDisruptorTemplate() {
        groupDisruptorTemplate = DisruptorTemplate.<GroupEvent>builder()
                .businessName("group-event")
                .ringBufferSize(8192)  // 8K，足够处理拼团事件
                .consumerThreads(4)  // 4个消费线程
                .waitStrategy(DisruptorWaitStrategy.BLOCKING)  // 阻塞等待策略，保证数据不丢失
                .batchEnabled(true)
                .batchSize(100)  // 每批100条
                .batchTimeout(200)  // 200ms超时
                .batchEventProcessor(groupEventProcessor)
                .build();

        groupDisruptorTemplate.start();

        log.info("拼团Disruptor初始化完成: ringBufferSize=8192, consumerThreads=4");

        return groupDisruptorTemplate;
    }

    @PreDestroy
    public void destroy() {
        if (groupDisruptorTemplate != null && groupDisruptorTemplate.isRunning()) {
            log.info("正在关闭拼团Disruptor...");
            groupDisruptorTemplate.stop();
            log.info("拼团Disruptor已关闭");
        }
    }
}

