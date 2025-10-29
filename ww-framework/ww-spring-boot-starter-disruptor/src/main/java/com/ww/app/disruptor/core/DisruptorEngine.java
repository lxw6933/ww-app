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
                log.info("正在启动Disruptor引擎，配置: {}", config);

                // 创建ThreadFactory
                ThreadFactory threadFactory = new DefaultThreadFactoryBuilder()
                        .setNamePrefix("disruptor-consumer")
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
                log.error("Disruptor引擎启动失败", e);
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
                    log.info("刷新批量缓冲区，剩余 {} 个事件", batchBuffer.size());
                    flushBatch();
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
                "disruptor-worker",
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

                // 达到批量大小时刷新
                if (batchBuffer.size() >= config.getBatchSize()) {
                    flushBatch();
                }
            }
            // 使用单个事件处理器
            else if (eventProcessor != null) {
                event.markProcessing();
                eventProcessor.process(event);
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
            }
        } catch (Exception e) {
            log.error("处理事件失败: {}, 线程: {}", event.getEventId(), Thread.currentThread().getName(), e);
            event.markFailed();
            failedCount.incrementAndGet();

            if (metricsCollector != null) {
                metricsCollector.recordFailure();
            }
        } finally {
            wrapper.clear();
        }
    }

    /**
     * 处理事件 - EventHandler接口方法（已废弃，保留用于兼容）
     * @deprecated 使用WorkerPool的handleEvent(EventWrapper)代替
     */
    @Deprecated
    private void handleEvent(EventWrapper<T> wrapper, long sequence, boolean endOfBatch) {
        Event<T> event = wrapper.getEvent();
        if (event == null) {
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            // 使用批量处理器
            if (batchEventProcessor != null && config.isBatchEnabled()) {
                batchBuffer.add(event);

                // 达到批量大小时刷新
                if (batchBuffer.size() >= config.getBatchSize()) {
                    flushBatch();
                }
            }
            // 使用单个事件处理器
            else if (eventProcessor != null) {
                event.markProcessing();
                eventProcessor.process(event);
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
            }
        } catch (Exception e) {
            log.error("处理事件失败: {}", event.getEventId(), e);
            event.markFailed();
            failedCount.incrementAndGet();

            if (metricsCollector != null) {
                metricsCollector.recordFailure();
            }
        } finally {
            wrapper.clear();
        }
    }

    /**
     * 刷新批量缓冲区 - 线程安全
     */
    private synchronized void flushBatch() {
        if (batchBuffer.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            // 从队列中取出所有事件
            List<Event<T>> eventsToProcess = new ArrayList<>();
            Event<T> event;
            while ((event = batchBuffer.poll()) != null) {
                eventsToProcess.add(event);
            }

            if (eventsToProcess.isEmpty()) {
                return;
            }

            EventBatch<T> batch = new EventBatch<>();
            batch.addEvents(eventsToProcess);
            batch.markProcessing();

            batchEventProcessor.processBatch(batch);
            batch.markCompleted();

            processCount.addAndGet(eventsToProcess.size());

            // 批量删除持久化数据
            if (persistenceManager != null) {
                for (Event<T> e : eventsToProcess) {
                    persistenceManager.remove(e.getEventId());
                }
            }

            // 记录批处理耗时
            long duration = System.currentTimeMillis() - startTime;
            if (metricsCollector != null) {
                metricsCollector.recordBatchProcessing(eventsToProcess.size(), duration);
            }

        } catch (Exception e) {
            log.error("批量处理失败", e);
            failedCount.incrementAndGet();

            if (metricsCollector != null) {
                metricsCollector.recordFailure();
            }
        }
    }

    /**
     * 启动批量处理定时器
     */
    private void startBatchScheduler() {
        this.batchScheduler = ThreadUtil.initScheduledExecutorService("disruptor-batch-scheduler", 1);
        batchScheduler.scheduleAtFixedRate(this::flushBatch, config.getBatchTimeout(), config.getBatchTimeout(), TimeUnit.MILLISECONDS);
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
        if (ringBuffer == null) {
            return 0;
        }
        long pending = ringBuffer.getBufferSize() - ringBuffer.remainingCapacity();
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
