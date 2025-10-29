package com.ww.app.disruptor.core;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.ww.app.disruptor.model.Event;
import com.ww.app.disruptor.model.EventBatch;
import com.ww.app.disruptor.processor.BatchEventProcessor;
import com.ww.app.disruptor.processor.EventProcessor;
import lombok.Getter;
import lombok.NonNull;
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

    /**
     * 线程池
     */
    private ExecutorService executor;

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

    /**
     * 批量事件缓冲区
     */
    private final List<Event<T>> batchBuffer = new CopyOnWriteArrayList<>();

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
                log.info("正在启动Disruptor引擎...");

                // 创建线程池
                this.executor = createExecutor();

                // 创建Disruptor
                this.disruptor = new Disruptor<>(
                        EventWrapper::new,
                        config.getRingBufferSize(),
                        executor,
                        ProducerType.MULTI,
                        createWaitStrategy()
                );

                // 设置事件处理器
                this.disruptor.handleEventsWith(this::handleEvent);

                // 设置异常处理器
                this.disruptor.setDefaultExceptionHandler(new DisruptorExceptionHandler());

                // 启动Disruptor
                this.ringBuffer = disruptor.start();

                // 启动批量处理定时器
                if (batchEventProcessor != null) {
                    startBatchScheduler();
                }

                log.info("Disruptor引擎启动成功，RingBuffer大小: {}", config.getRingBufferSize());
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
                    batchScheduler.shutdown();
                }

                // 处理剩余的批量事件
                if (!batchBuffer.isEmpty()) {
                    flushBatch();
                }

                // 关闭Disruptor
                if (disruptor != null) {
                    disruptor.shutdown();
                }

                // 关闭线程池
                if (executor != null) {
                    executor.shutdown();
                    executor.awaitTermination(5, TimeUnit.SECONDS);
                }

                log.info("Disruptor引擎已停止，总发布: {}, 总处理: {}", publishCount.get(), processCount.get());
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

        try {
            long sequence = ringBuffer.next();
            try {
                EventWrapper<T> wrapper = ringBuffer.get(sequence);
                wrapper.setEvent(event);
            } finally {
                ringBuffer.publish(sequence);
            }
            publishCount.incrementAndGet();
            return true;
        } catch (Exception e) {
            log.error("发布事件失败: {}", event.getEventId(), e);
            return false;
        }
    }

    /**
     * 尝试发布事件（非阻塞）
     */
    public boolean tryPublishEvent(Event<T> event, long timeout, TimeUnit unit) {
        if (!started.get()) {
            return false;
        }

        try {
            long sequence = ringBuffer.tryNext();
            try {
                EventWrapper<T> wrapper = ringBuffer.get(sequence);
                wrapper.setEvent(event);
            } finally {
                ringBuffer.publish(sequence);
            }
            publishCount.incrementAndGet();
            return true;
        } catch (InsufficientCapacityException e) {
            log.warn("RingBuffer容量不足，发布事件失败");
            return false;
        }
    }

    /**
     * 处理事件
     */
    private void handleEvent(EventWrapper<T> wrapper, long sequence, boolean endOfBatch) {
        Event<T> event = wrapper.getEvent();
        if (event == null) {
            return;
        }

        try {
            // 使用批量处理器
            if (batchEventProcessor != null) {
                batchBuffer.add(event);

                // 达到批量大小或批次结束时刷新
                if (batchBuffer.size() >= config.getBatchSize() || endOfBatch) {
                    flushBatch();
                }
            }
            // 使用单个事件处理器
            else if (eventProcessor != null) {
                event.markProcessing();
                eventProcessor.process(event);
                event.markCompleted();
                processCount.incrementAndGet();
            }
        } catch (Exception e) {
            log.error("处理事件失败: {}", event.getEventId(), e);
            event.markFailed();
        } finally {
            wrapper.clear();
        }
    }

    /**
     * 刷新批量缓冲区
     */
    private void flushBatch() {
        if (batchBuffer.isEmpty()) {
            return;
        }

        try {
            List<Event<T>> eventsToProcess = new ArrayList<>(batchBuffer);
            batchBuffer.clear();

            EventBatch<T> batch = new EventBatch<>();
            batch.addEvents(eventsToProcess);
            batch.markProcessing();

            batchEventProcessor.processBatch(batch);
            batch.markCompleted();

            processCount.addAndGet(eventsToProcess.size());
        } catch (Exception e) {
            log.error("批量处理失败", e);
        }
    }

    /**
     * 启动批量处理定时器
     */
    private void startBatchScheduler() {
        this.batchScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "disruptor-batch-scheduler");
            thread.setDaemon(true);
            return thread;
        });

        batchScheduler.scheduleAtFixedRate(
                this::flushBatch,
                config.getBatchTimeout(),
                config.getBatchTimeout(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 创建线程池
     */
    private ExecutorService createExecutor() {
        return new ThreadPoolExecutor(
                config.getConsumerThreads(),
                config.getConsumerThreads(),
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1024),
                new ThreadFactory() {
                    private final AtomicLong counter = new AtomicLong(0);

                    @Override
                    public Thread newThread(@NonNull Runnable r) {
                        Thread thread = new Thread(r, "disruptor-consumer-" + counter.incrementAndGet());
                        thread.setDaemon(false);
                        return thread;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 创建等待策略
     */
    private WaitStrategy createWaitStrategy() {
        switch (config.getWaitStrategy()) {
            case "BLOCKING":
                return new BlockingWaitStrategy();
            case "YIELDING":
                return new YieldingWaitStrategy();
            case "SLEEPING":
                return new SleepingWaitStrategy();
            case "BUSY_SPIN":
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
        return ringBuffer.getBufferSize() - ringBuffer.remainingCapacity();
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

    /**
     * 事件包装器
     */
    @Setter
    @Getter
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
