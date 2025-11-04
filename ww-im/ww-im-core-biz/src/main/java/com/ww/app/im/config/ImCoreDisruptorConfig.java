package com.ww.app.im.config;

import com.ww.app.disruptor.api.DisruptorTemplate;
import com.ww.app.disruptor.constans.DisruptorWaitStrategy;
import com.ww.app.im.event.ImMsgEvent;
import com.ww.app.im.processor.ImMsgBatchEventProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * IM Core Disruptor配置
 * @author ww
 */
@Slf4j
@Configuration
public class ImCoreDisruptorConfig {

    @Resource
    private ImMsgBatchEventProcessor imMsgBatchEventProcessor;
    
    private DisruptorTemplate<ImMsgEvent> imMsgDisruptorTemplate;

    @Bean
    public DisruptorTemplate<ImMsgEvent> imMsgDisruptorTemplate() {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        
        DisruptorTemplate<ImMsgEvent> template = DisruptorTemplate.<ImMsgEvent>builder()
                .businessName("im-msg")
                .ringBufferSize(16384)  // 16K，2的幂次方
                .consumerThreads(cpuCores * 2)  // 双倍CPU核心数
                .waitStrategy(DisruptorWaitStrategy.YIELDING)  // 低延迟策略
                .batchEnabled(true)
                .batchSize(100)  // 每批100条
                .batchTimeout(10)  // 10ms超时
                .batchEventProcessor(imMsgBatchEventProcessor)
                .build();
        
        template.start();
        
        this.imMsgDisruptorTemplate = template;
        
        log.info("IM Core Disruptor 初始化完成: consumerThreads={}", cpuCores * 2);
        
        return template;
    }
    
    @PreDestroy
    public void destroy() {
        if (imMsgDisruptorTemplate != null) {
            log.info("正在关闭 IM Core Disruptor...");
            imMsgDisruptorTemplate.stop();
        }
    }
}
