package com.ww.app.disruptor.api;

import com.ww.app.disruptor.core.DisruptorConfig;
import com.ww.app.disruptor.core.DisruptorEngine;
import com.ww.app.disruptor.model.Event;
import com.ww.app.disruptor.processor.BatchEventProcessor;
import com.ww.app.disruptor.processor.EventProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Disruptor模板类 - 提供简单易用的API
 * 类似于RestTemplate的设计思想
 *
 * @author ww-framework
 */
@Slf4j
public class DisruptorTemplate<T> {

    private final DisruptorEngine<T> engine;

    private DisruptorTemplate(DisruptorEngine<T> engine) {
        this.engine = engine;
    }

    /**
     * 发布事件
     */
    public boolean publish(Event<T> event) {
        return engine.publishEvent(event);
    }

    /**
     * 发布事件（简化版）
     */
    public boolean publish(String eventType, T payload) {
        Event<T> event = new Event<>(eventType, payload);
        return engine.publishEvent(event);
    }

    /**
     * 异步发布事件
     */
    public CompletableFuture<Boolean> publishAsync(Event<T> event) {
        return CompletableFuture.supplyAsync(() -> engine.publishEvent(event));
    }

    /**
     * 尝试发布事件（非阻塞）
     */
    public boolean tryPublish(Event<T> event, long timeout, TimeUnit unit) {
        return engine.tryPublishEvent(event, timeout, unit);
    }

    /**
     * 启动引擎
     */
    public void start() {
        engine.start();
    }

    /**
     * 停止引擎
     */
    public void stop() {
        engine.stop();
    }

    /**
     * 获取引擎状态
     */
    public boolean isRunning() {
        return engine.isStarted();
    }

    /**
     * 获取发布计数
     */
    public long getPublishCount() {
        return engine.getPublishCount();
    }

    /**
     * 获取处理计数
     */
    public long getProcessCount() {
        return engine.getProcessCount();
    }

    /**
     * 获取队列利用率
     */
    public double getQueueUtilization() {
        return engine.getQueueUtilization();
    }

    /**
     * 获取待处理事件数
     */
    public long getPendingCount() {
        return engine.getPendingCount();
    }

    /**
     * 创建Builder
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Builder类
     */
    public static class Builder<T> {
        private final DisruptorConfig config = new DisruptorConfig();
        private EventProcessor<T> eventProcessor;
        private BatchEventProcessor<T> batchEventProcessor;

        public Builder<T> ringBufferSize(int size) {
            config.setRingBufferSize(size);
            return this;
        }

        public Builder<T> consumerThreads(int threads) {
            config.setConsumerThreads(threads);
            return this;
        }

        public Builder<T> batchSize(int size) {
            config.setBatchSize(size);
            return this;
        }

        public Builder<T> batchTimeout(long timeout) {
            config.setBatchTimeout(timeout);
            return this;
        }

        public Builder<T> waitStrategy(String strategy) {
            config.setWaitStrategy(strategy);
            return this;
        }

        public Builder<T> batchEnabled(boolean enabled) {
            config.setBatchEnabled(enabled);
            return this;
        }

        public Builder<T> eventProcessor(EventProcessor<T> processor) {
            this.eventProcessor = processor;
            return this;
        }

        public Builder<T> batchEventProcessor(BatchEventProcessor<T> processor) {
            this.batchEventProcessor = processor;
            return this;
        }

        public Builder<T> businessName(String name) {
            config.setBusinessName(name);
            return this;
        }

        public DisruptorTemplate<T> build() {
            // 验证配置
            config.validate();

            // 创建引擎
            DisruptorEngine<T> engine = new DisruptorEngine<>(config);

            // 设置处理器
            if (batchEventProcessor != null) {
                engine.setBatchEventProcessor(batchEventProcessor);
            } else if (eventProcessor != null) {
                engine.setEventProcessor(eventProcessor);
            } else {
                throw new IllegalStateException("必须设置至少一个事件处理器");
            }

            return new DisruptorTemplate<>(engine);
        }
    }
}
