package com.ww.app.disruptor.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 监控指标收集器 - 集成Micrometer
 * 
 * @author ww-framework
 */
@Slf4j
public class MetricsCollector {
    
    private final MeterRegistry meterRegistry;
    private final String metricsPrefix;
    
    // 计数器
    private Counter publishCounter;
    private Counter processCounter;
    private Counter failureCounter;
    private Counter batchCounter;
    
    // 计时器
    private Timer processingTimer;
    private Timer batchProcessingTimer;
    
    // 仪表盘（实时值）
    private final AtomicLong pendingCount = new AtomicLong(0);
    private final LongAdder totalPublished = new LongAdder();
    private final LongAdder totalProcessed = new LongAdder();
    private final LongAdder totalFailed = new LongAdder();
    
    public MetricsCollector(MeterRegistry meterRegistry, String metricsPrefix) {
        this.meterRegistry = meterRegistry;
        this.metricsPrefix = metricsPrefix;
    }
    
    /**
     * 启动监控
     */
    public void start() {
        try {
            // 初始化计数器
            publishCounter = Counter.builder(metricsPrefix + ".events.published")
                    .description("发布事件总数")
                    .register(meterRegistry);
            
            processCounter = Counter.builder(metricsPrefix + ".events.processed")
                    .description("处理事件总数")
                    .register(meterRegistry);
            
            failureCounter = Counter.builder(metricsPrefix + ".events.failed")
                    .description("失败事件总数")
                    .register(meterRegistry);
            
            batchCounter = Counter.builder(metricsPrefix + ".batches.processed")
                    .description("批处理次数")
                    .register(meterRegistry);
            
            // 初始化计时器
            processingTimer = Timer.builder(metricsPrefix + ".event.processing.time")
                    .description("事件处理耗时")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry);
            
            batchProcessingTimer = Timer.builder(metricsPrefix + ".batch.processing.time")
                    .description("批处理耗时")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry);
            
            // 注册仪表盘
            Gauge.builder(metricsPrefix + ".events.pending", pendingCount, AtomicLong::get)
                    .description("待处理事件数")
                    .register(meterRegistry);
            
            Gauge.builder(metricsPrefix + ".throughput.published", totalPublished, LongAdder::sum)
                    .description("发布吞吐量")
                    .register(meterRegistry);
            
            Gauge.builder(metricsPrefix + ".throughput.processed", totalProcessed, LongAdder::sum)
                    .description("处理吞吐量")
                    .register(meterRegistry);
            
            log.info("监控指标收集器启动成功，指标前缀: {}", metricsPrefix);
        } catch (Exception e) {
            log.error("监控指标收集器启动失败", e);
        }
    }
    
    /**
     * 停止监控
     */
    public void stop() {
        try {
            // 打印最终统计
            log.info("监控统计 - 总发布: {}, 总处理: {}, 总失败: {}", 
                    totalPublished.sum(), totalProcessed.sum(), totalFailed.sum());
        } catch (Exception e) {
            log.error("监控指标收集器停止异常", e);
        }
    }
    
    /**
     * 记录事件发布
     */
    public void recordPublish() {
        publishCounter.increment();
        totalPublished.increment();
        pendingCount.incrementAndGet();
    }
    
    /**
     * 记录事件处理
     */
    public void recordProcessing(long durationMillis) {
        processCounter.increment();
        totalProcessed.increment();
        pendingCount.decrementAndGet();
        processingTimer.record(durationMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    /**
     * 记录批量处理
     */
    public void recordBatchProcessing(int batchSize, long durationMillis) {
        batchCounter.increment();
        processCounter.increment(batchSize);
        totalProcessed.add(batchSize);
        pendingCount.addAndGet(-batchSize);
        batchProcessingTimer.record(durationMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    /**
     * 记录失败
     */
    public void recordFailure() {
        failureCounter.increment();
        totalFailed.increment();
        pendingCount.decrementAndGet();
    }
    
    /**
     * 更新待处理数量
     */
    public void updatePendingCount(long count) {
        pendingCount.set(count);
    }
    
    /**
     * 获取统计信息
     */
    public MetricsSnapshot getSnapshot() {
        return new MetricsSnapshot(
                totalPublished.sum(),
                totalProcessed.sum(),
                totalFailed.sum(),
                pendingCount.get(),
                processingTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS),
                batchProcessingTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS)
        );
    }
    
    /**
     * 监控快照
     */
    @Data
    public static class MetricsSnapshot {
        // Getters
        private final long totalPublished;
        private final long totalProcessed;
        private final long totalFailed;
        private final long pendingCount;
        private final double avgProcessingTime;
        private final double avgBatchProcessingTime;
        
        public MetricsSnapshot(long totalPublished, long totalProcessed, long totalFailed, 
                              long pendingCount, double avgProcessingTime, double avgBatchProcessingTime) {
            this.totalPublished = totalPublished;
            this.totalProcessed = totalProcessed;
            this.totalFailed = totalFailed;
            this.pendingCount = pendingCount;
            this.avgProcessingTime = avgProcessingTime;
            this.avgBatchProcessingTime = avgBatchProcessingTime;
        }
    }
}
