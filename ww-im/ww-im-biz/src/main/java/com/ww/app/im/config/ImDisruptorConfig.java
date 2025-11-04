package com.ww.app.im.config;

import com.ww.app.disruptor.api.DisruptorTemplate;
import com.ww.app.disruptor.constans.DisruptorWaitStrategy;
import com.ww.app.im.entity.SingleChatMessage;
import com.ww.app.im.processor.ImMsgPersistenceProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * IM Biz Disruptor配置 - MongoDB持久化
 * @author ww
 */
@Slf4j
@Configuration
public class ImDisruptorConfig {

    @Resource
    private ImMsgPersistenceProcessor imMsgPersistenceProcessor;
    
    private DisruptorTemplate<SingleChatMessage> persistenceDisruptorTemplate;

    @Bean
    public DisruptorTemplate<SingleChatMessage> persistenceDisruptorTemplate() {
        DisruptorTemplate<SingleChatMessage> template = DisruptorTemplate.<SingleChatMessage>builder()
                .businessName("im-persistence")
                .ringBufferSize(32768)  // 32K
                .consumerThreads(4)  // 4个消费线程
                .waitStrategy(DisruptorWaitStrategy.BLOCKING)  // 可以等待，不要求极低延迟
                .batchEnabled(true)
                .batchSize(500)  // 每批500条
                .batchTimeout(100)  // 100ms超时，允许稍高延迟
                .batchEventProcessor(imMsgPersistenceProcessor)
                .build();
        
        template.start();
        
        this.persistenceDisruptorTemplate = template;
        
        log.info("IM Persistence Disruptor 初始化完成");
        
        return template;
    }
    
    @PreDestroy
    public void destroy() {
        if (persistenceDisruptorTemplate != null) {
            log.info("正在关闭 IM Persistence Disruptor...");
            persistenceDisruptorTemplate.stop();
        }
    }
}
