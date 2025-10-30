package com.ww.app.disruptor.core;

import com.lmax.disruptor.*;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.ww.app.common.thread.DefaultThreadFactoryBuilder;
import com.ww.app.common.utils.ThreadUtil;
import com.ww.app.disruptor.constans.DisruptorWaitStrategy;
import com.ww.app.disruptor.model.Event;
import com.ww.app.disruptor.model.EventBatch;
import com.ww.app.disruptor.model.ProcessResult;
import com.ww.app.disruptor.monitor.MetricsCollector;
import com.ww.app.disruptor.persistence.PersistenceManager;
import com.ww.app.disruptor.processor.BatchEventProcessor;
import com.ww.app.disruptor.processor.EventProcessor;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Disruptor引擎核心类 - 管理RingBuffer和事件处理
 *
 * @author ww-framework
 */
@Slf4j
public class DisruptorEngine<T> {

    /**
     * Disruptor实例
     */
    private Disruptor<EventWrapper<T>> disruptor;

    /**
     * RingBuffer
     */
    private RingBuffer<EventWrapper<T>> ringBuffer;

    /**
     * 事件处理器
     */
    @Setter
    private EventProcessor<T> eventProcessor;

    /**
     * 批量事件处理器
     */
    @Setter
    private BatchEventProcessor<T> batchEventProcessor;

    @Setter
    private PersistenceManager<T> persistenceManager;

    @Setter
    private MetricsCollector metricsCollector;

    /**
     * WorkerPool用于多消费者并发处理
     */
    private WorkerPool<EventWrapper<T>> workerPool;

    /**
     * 消费者线程池
     */
    private ExecutorService consumerExecutor;

    /**
     * 引擎配置
     */
    private final DisruptorConfig config;

    /**
     * 引擎状态
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * 发布计数器
     */
    private final AtomicLong publishCount = new AtomicLong(0);

    /**
     * 处理计数器
     */
    private final AtomicLong processCount = new AtomicLong(0);
    private final AtomicLong failedCount = new AtomicLong(0);

    /**
     * 批量事件缓冲区 - 使用线程安全的队列
     */
    private final ConcurrentLinkedQueue<Event<T>> batchBuffer = new ConcurrentLinkedQueue<>();

    /**
     * 批量处理定时器
     */
    private ScheduledExecutorService batchScheduler;

    public DisruptorEngine(DisruptorConfig config) {
        this.config = config;
    }

    /**
     * 启动引擎
     */
    public void start() {
        if (started.compareAndSet(false, true)) {
            try {
                // 配置验证
                config.validate();
                
                // 处理器验证
                if (eventProcessor == null && batchEventProcessor == null) {
                    throw new IllegalStateException("至少需要设置 eventProcessor 或 batchEventProcessor 之一");
                }
                
                if (config.isBatchEnabled() && batchEventProcessor == null) {
                    throw new IllegalStateException("批量处理已启用但未设置 batchEventProcessor");
                }
                
                log.info("正在启动Disruptor引擎，配置: {}", config);

                // 创建ThreadFactory
                ThreadFactory threadFactory = new DefaultThreadFactoryBuilder()
                        .setNamePrefix(config.getBusinessName() + "-disruptor-consumer")
                        .build();

                // 创建Disruptor
                this.disruptor = new Disruptor<>(
                        EventWrapper::new,
                        config.getRingBufferSize(),
                        threadFactory,
                        ProducerType.MULTI,
                        createWaitStrategy()
                );

                // 设置异常处理器
                this.disruptor.setDefaultExceptionHandler(new DisruptorExceptionHandler());

                // 启动Disruptor获取RingBuffer
                this.ringBuffer = disruptor.start();

                // 使用WorkerPool实现多消费者并发处理
                setupWorkerPool();

                // 启动持久化管理器
                if (persistenceManager != null) {
                    persistenceManager.start();
                    // 恢复未处理的事件
                    List<Event<T>> recovered = persistenceManager.recover();
                    for (Event<T> event : recovered) {
                        publishEvent(event);
                    }
                    log.info("恢复了 {} 个未处理的事件", recovered.size());
                }

                // 启动监控
                if (metricsCollector != null) {
                    metricsCollector.start();
                }

                // 启动批量处理定时器
                if (batchEventProcessor != null && config.isBatchEnabled()) {
                    startBatchScheduler();
                }

                log.info("Disruptor引擎启动成功，RingBuffer大小: {}, 消费者线程数: {}", 
                        config.getRingBufferSize(), config.getConsumerThreads());
            } catch (Exception e) {
                started.set(false);
                log.error("Disruptor引擎启动失败，开始清理资源", e);
                
                // 清理已创建的资源
                cleanupResources();
                
                throw new RuntimeException("Disruptor引擎启动失败", e);
            }
        }
    }

    /**
     * 停止引擎
     */
    public void stop() {
        if (started.compareAndSet(true, false)) {
            try {
                log.info("正在停止Disruptor引擎...");

                // 停止批量处理定时器
                if (batchScheduler != null) {
                    ThreadUtil.shutdown("批量调度器", this::flushBatch, batchScheduler);
                }

                // 处理剩余的批量事件
                if (!batchBuffer.isEmpty()) {
                    int remainingCount = batchBuffer.size();
                    log.info("刷新批量缓冲区，剩余 {} 个事件", remainingCount);
                    
                    try {
                        flushBatch();
                    } catch (Exception e) {
                        log.error("停止时刷新批量缓冲区失败，尝试持久化剩余事件", e);
                        
                        // 失败时，至少持久化事件
                        if (persistenceManager != null) {
                            Event<T> event;
                            while ((event = batchBuffer.poll()) != null) {
                                try {
                                    persistenceManager.persist(event);
                                } catch (Exception pe) {
                                    log.error("持久化事件失败: {}", event.getEventId(), pe);
                                }
                            }
                        }
                    }
                }

                // 停止WorkerPool
                if (workerPool != null) {
                    try {
                        long pendingCount = getPendingCount();
                        if (pendingCount > 0) {
                            log.info("等待处理 {} 个剩余事件...", pendingCount);
                        }

                        // 停止WorkerPool
                        workerPool.drainAndHalt();
                        log.info("WorkerPool已停止");
                    } catch (Exception e) {
                        log.error("停止WorkerPool异常", e);
                    }
                }

                // 关闭Disruptor
                if (disruptor != null) {
                    try {
                        // 带超时的shutdown，避免无限等待
                        disruptor.shutdown(30, TimeUnit.SECONDS);
                        log.info("Disruptor已优雅关闭");

                    } catch (TimeoutException e) {
                        log.warn("Disruptor关闭超时，强制关闭");
                        disruptor.halt();
                    }
                }

                // 关闭消费者线程池
                if (consumerExecutor != null) {
                    ThreadUtil.shutdown("消费者线程池", () -> log.info("消费者线程池清理完成"), consumerExecutor);
                }

                // 停止持久化管理器
                if (persistenceManager != null) {
                    persistenceManager.stop();
                }

                // 停止监控
                if (metricsCollector != null) {
                    metricsCollector.stop();
                }

                log.info("Disruptor引擎已停止，总发布: {}, 总处理: {}, 总失败: {}", publishCount.get(), processCount.get(), failedCount.get());
            } catch (Exception e) {
                log.error("Disruptor引擎停止异常", e);
            }
        }
    }

    /**
     * 发布事件
     */
    public boolean publishEvent(Event<T> event) {
        if (!started.get()) {
            log.warn("引擎未启动，无法发布事件");
            return false;
        }

        if (event == null) {
            log.warn("事件为null，无法发布");
            return false;
        }

        try {
            // 先持久化（防止数据丢失）
            if (persistenceManager != null) {
                persistenceManager.persist(event);
            }

            // 使用最新API发布事件
            ringBuffer.publishEvent((wrapper, sequence) -> wrapper.setEvent(event));

            publishCount.incrementAndGet();

            // 记录监控指标
            if (metricsCollector != null) {
                metricsCollector.recordPublish();
            }

            return true;
        } catch (Exception e) {
            log.error("发布事件失败: {}", event.getEventId(), e);
            failedCount.incrementAndGet();

            if (metricsCollector != null) {
                metricsCollector.recordFailure();
            }

            return false;
        }
    }

    /**
     * 尝试发布事件（非阻塞，带超时）
     */
    public boolean tryPublishEvent(Event<T> event, long timeout, TimeUnit unit) {
        if (!started.get()) {
            log.warn("引擎未启动，无法发布事件");
            return false;
        }

        if (event == null) {
            log.warn("事件为null，无法发布");
            return false;
        }

        if (timeout <= 0) {
            log.warn("超时时间必须大于0");
            return false;
        }

        long deadlineNanos = System.nanoTime() + unit.toNanos(timeout);

        try {
            // 先持久化
            if (persistenceManager != null) {
                persistenceManager.persist(event);
            }

            // 带超时重试的tryPublishEvent
            while (System.nanoTime() < deadlineNanos) {
                try {
                    boolean success = ringBuffer.tryPublishEvent((wrapper, sequence) ->
                            wrapper.setEvent(event)
                    );

                    if (success) {
                        publishCount.incrementAndGet();
                        if (metricsCollector != null) {
                            metricsCollector.recordPublish();
                        }
                        return true;
                    }

                    // 短暂休眠后重试
                    TimeUnit.MILLISECONDS.sleep(1);
                } catch (Exception e) {
                    // RingBuffer满，继续重试
                }
            }

            log.warn("发布事件超时: {} (timeout={}{})", event.getEventId(), timeout, unit);
            return false;

        } catch (Exception e) {
            log.error("尝试发布事件失败: {}", event.getEventId(), e);
            failedCount.incrementAndGet();
            return false;
        }
    }

    /**
     * 设置WorkerPool实现多消费者并发
     */
    private void setupWorkerPool() {
        // 创建多个WorkHandler实例
        @SuppressWarnings("unchecked")
        WorkHandler<EventWrapper<T>>[] handlers = new WorkHandler[config.getConsumerThreads()];
        for (int i = 0; i < config.getConsumerThreads(); i++) {
            handlers[i] = this::handleEvent;
        }

        // 创建WorkerPool
        this.workerPool = new WorkerPool<>(
                ringBuffer,
                ringBuffer.newBarrier(),
                new DisruptorExceptionHandler(),
                handlers
        );

        // 添加Gating Sequences
        ringBuffer.addGatingSequences(workerPool.getWorkerSequences());

        // 创建消费者线程池
        this.consumerExecutor = ThreadUtil.initFixedThreadPoolExecutor(
                config.getBusinessName() + "-disruptor-worker",
                config.getConsumerThreads()
        );

        // 启动WorkerPool
        workerPool.start(consumerExecutor);

        log.info("WorkerPool已启动，消费者线程数: {}", config.getConsumerThreads());
    }

    /**
     * 处理事件 - WorkHandler接口方法
     */
    private void handleEvent(EventWrapper<T> wrapper) {
        Event<T> event = wrapper.getEvent();
        if (event == null) {
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            // 使用批量处理器
            if (batchEventProcessor != null && config.isBatchEnabled()) {
                batchBuffer.offer(event);

                // 达到批量大小时刷新（双重检查防止竞态）
                if (batchBuffer.size() >= config.getBatchSize()) {
                    synchronized (this) {
                        if (batchBuffer.size() >= config.getBatchSize()) {
                            flushBatch();
                        }
                    }
                }
            }
            // 使用单个事件处理器
            else if (eventProcessor != null) {
                event.markProcessing();
                ProcessResult result = eventProcessor.process(event);
                
                // 根据处理结果决定后续操作
                if (result.isSuccess()) {
                    event.markCompleted();
                    processCount.incrementAndGet();

                    // 处理成功后删除持久化数据
                    if (persistenceManager != null) {
                        persistenceManager.remove(event.getEventId());
                    }

                    // 记录处理耗时
                    long duration = System.currentTimeMillis() - startTime;
                    if (metricsCollector != null) {
                        metricsCollector.recordProcessing(duration);
                    }
                } else {
                    // 处理失败
                    event.markFailed();
                    failedCount.incrementAndGet();
                    log.error("事件处理失败: {}, 原因: {}", event.getEventId(), result.getMessage());
                    
                    if (metricsCollector != null) {
                        metricsCollector.recordFailure();
                    }
                }
            }
        } catch (Exception e) {
            log.error("处理事件异常: {}, 线程: {}", event.getEventId(), Thread.currentThread().getName(), e);
            event.markFailed();
            // 注意：failedCount 和 metrics 统一由 ExceptionHandler 处理，避免重复计数
            
            // 重新抛出让ExceptionHandler处理
            throw new RuntimeException(e);
        } finally {
            wrapper.clear();
        }
    }

    /**
     * 刷新批量缓冲区 - 线程安全
     */
    private synchronized void flushBatch() {
        // 直接尝试取出所有事件
        List<Event<T>> eventsToProcess = new ArrayList<>();
        Event<T> event;
        while ((event = batchBuffer.poll()) != null) {
            eventsToProcess.add(event);
        }

        if (eventsToProcess.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        EventBatch<T> batch = new EventBatch<>();
        batch.addEvents(eventsToProcess);
        batch.markProcessing();

        try {
            // 执行批处理并获取结果
            ProcessResult result = batchEventProcessor.processBatch(batch);
            
            // 根据结果分别处理
            if (result.isSuccess()) {
                batch.markCompleted();
                processCount.addAndGet(eventsToProcess.size());

                // 成功后删除持久化数据
                if (persistenceManager != null) {
                    for (Event<T> e : eventsToProcess) {
                        persistenceManager.remove(e.getEventId());
                    }
                }

                // 记录成功指标
                long duration = System.currentTimeMillis() - startTime;
                if (metricsCollector != null) {
                    metricsCollector.recordBatchProcessing(eventsToProcess.size(), duration);
                }
            } else {
                // 处理失败 - 标记批次和所有事件为失败
                batch.markFailed();
                for (Event<T> e : eventsToProcess) {
                    e.markFailed();
                }
                failedCount.addAndGet(eventsToProcess.size());
                log.error("批量处理失败: {}, 影响事件数: {}", result.getMessage(), eventsToProcess.size());
                
                // 失败时持久化事件，以便后续重试
                if (persistenceManager != null) {
                    for (Event<T> e : eventsToProcess) {
                        try {
                            persistenceManager.persist(e);
                        } catch (Exception pe) {
                            log.error("持久化失败事件异常: {}", e.getEventId(), pe);
                        }
                    }
                }
                
                if (metricsCollector != null) {
                    metricsCollector.recordFailure();
                }
            }
        } catch (Exception e) {
            log.error("批量处理异常", e);
            batch.markFailed();
            
            // 标记所有事件为失败并持久化
            for (Event<T> evt : eventsToProcess) {
                evt.markFailed();
                if (persistenceManager != null) {
                    try {
                        persistenceManager.persist(evt);
                    } catch (Exception pe) {
                        log.error("持久化失败事件异常: {}", evt.getEventId(), pe);
                    }
                }
            }
            
            failedCount.addAndGet(eventsToProcess.size());
            
            if (metricsCollector != null) {
                metricsCollector.recordFailure();
            }
        }
    }

    /**
     * 启动批量处理定时器
     */
    private void startBatchScheduler() {
        this.batchScheduler = ThreadUtil.initScheduledExecutorService(config.getBusinessName() + "-disruptor-batch-scheduler", 1);
        // 立即执行第一次，然后按周期执行
        batchScheduler.scheduleAtFixedRate(this::flushBatch, 0, config.getBatchTimeout(), TimeUnit.MILLISECONDS);
    }

    /**
     * 清理资源 - 用于启动失败时的资源清理
     */
    private void cleanupResources() {
        try {
            // 停止批量处理定时器
            if (batchScheduler != null) {
                try {
                    batchScheduler.shutdownNow();
                    batchScheduler = null;
                } catch (Exception e) {
                    log.error("清理批量调度器失败", e);
                }
            }
            
            // 停止WorkerPool
            if (workerPool != null) {
                try {
                    workerPool.halt();
                    workerPool = null;
                } catch (Exception e) {
                    log.error("清理WorkerPool失败", e);
                }
            }
            
            // 关闭Disruptor
            if (disruptor != null) {
                try {
                    disruptor.halt();
                    disruptor = null;
                } catch (Exception e) {
                    log.error("清理Disruptor失败", e);
                }
            }
            
            // 关闭消费者线程池
            if (consumerExecutor != null) {
                try {
                    consumerExecutor.shutdownNow();
                    consumerExecutor = null;
                } catch (Exception e) {
                    log.error("清理消费者线程池失败", e);
                }
            }
            
            // 停止持久化管理器
            if (persistenceManager != null) {
                try {
                    persistenceManager.stop();
                } catch (Exception e) {
                    log.error("清理持久化管理器失败", e);
                }
            }
            
            // 停止监控
            if (metricsCollector != null) {
                try {
                    metricsCollector.stop();
                } catch (Exception e) {
                    log.error("清理监控收集器失败", e);
                }
            }
            
            // 清空缓冲区
            batchBuffer.clear();
            ringBuffer = null;
            
            log.info("资源清理完成");
        } catch (Exception e) {
            log.error("资源清理过程发生异常", e);
        }
    }

    /**
     * 创建等待策略
     */
    private WaitStrategy createWaitStrategy() {
        switch (config.getWaitStrategy()) {
            case DisruptorWaitStrategy.BLOCKING:
                return new BlockingWaitStrategy();
            case DisruptorWaitStrategy.YIELDING:
                return new YieldingWaitStrategy();
            case DisruptorWaitStrategy.SLEEPING:
                return new SleepingWaitStrategy();
            case DisruptorWaitStrategy.BUSY_SPIN:
                return new BusySpinWaitStrategy();
            default:
                return new BlockingWaitStrategy();
        }
    }

    /**
     * 获取待处理事件数量
     */
    public long getPendingCount() {
        if (ringBuffer == null || workerPool == null) {
            return 0;
        }
        
        // 获取生产者游标（已发布的最大序号）
        long cursor = ringBuffer.getCursor();
        
        // 获取消费者最慢的序号
        Sequence[] sequences = workerPool.getWorkerSequences();
        long minSequence = cursor;
        for (Sequence sequence : sequences) {
            long value = sequence.get();
            if (value < minSequence) {
                minSequence = value;
            }
        }
        
        // 待处理 = 生产者游标 - 最慢消费者
        long pending = cursor - minSequence;
        return Math.max(0, pending);
    }

    /**
     * 获取队列利用率
     */
    public double getQueueUtilization() {
        if (ringBuffer == null) {
            return 0.0;
        }
        return (double) getPendingCount() / ringBuffer.getBufferSize() * 100;
    }

    public boolean isStarted() {
        return started.get();
    }

    public long getPublishCount() {
        return publishCount.get();
    }

    public long getProcessCount() {
        return processCount.get();
    }

    public long getFailedCount() {
        return failedCount.get();
    }

    /**
     * 事件包装器
     */
    @Data
    public static class EventWrapper<T> {
        private Event<T> event;

        public void clear() {
            this.event = null;
        }
    }

    /**
     * 异常处理器
     */
    private class DisruptorExceptionHandler implements ExceptionHandler<EventWrapper<T>> {
        @Override
        public void handleEventException(Throwable ex, long sequence, EventWrapper<T> event) {
            log.error("处理事件异常, sequence: {}, event: {}", sequence, event.getEvent(), ex);
            failedCount.incrementAndGet();
            if (metricsCollector != null) {
                metricsCollector.recordFailure();
            }
        }

        @Override
        public void handleOnStartException(Throwable ex) {
            log.error("Disruptor启动异常", ex);
        }

        @Override
        public void handleOnShutdownException(Throwable ex) {
            log.error("Disruptor关闭异常", ex);
        }
    }
}
