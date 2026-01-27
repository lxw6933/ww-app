package com.ww.app.rabbitmq.template;

import cn.hutool.core.collection.CollUtil;
import com.rabbitmq.client.Channel;
import com.ww.app.disruptor.api.DisruptorTemplate;
import com.ww.app.disruptor.constans.DisruptorWaitStrategy;
import com.ww.app.disruptor.model.Event;
import com.ww.app.disruptor.model.EventBatch;
import com.ww.app.disruptor.model.ProcessResult;
import com.ww.app.disruptor.processor.BatchEventProcessor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author: ww
 * @create: 2023/7/22 22:32
 * @description: 批量操作消费者模板 - 单条交付，使用Disruptor进行聚合后批量处理
 **/
@Slf4j
public abstract class BatchOperationConsumerTemplate<T, R> extends AbstractMsgConsumerTemplate<T> {

    /**
     * 最小批处理阈值，低于此阈值不执行批处理
     */
    private static final int DEFAULT_MIN_BATCH_SIZE = 10;

    /**
     * Ack队列执行器（每个Channel一个单线程）
     */
    private final Map<Channel, ExecutorService> ackExecutors = new ConcurrentHashMap<>();

    /**
     * Disruptor聚合模板
     */
    private volatile DisruptorTemplate<BatchAckContext<T>> disruptorTemplate;

    /**
     * 单条消息消费入口（单条交付 + Disruptor 聚合）
     */
    public final void consumer(Message message, T msg, Channel channel) throws IOException {
        MessageProperties properties = message.getMessageProperties();
        long deliveryTag = properties.getDeliveryTag();

        try {
            if (!isValidMessage(message, msg)) {
                log.error("无效消息[{}][{}]直接确认消费[tag: {}]", message, msg, deliveryTag);
                enqueueAck(channel, deliveryTag);
                return;
            }

            BatchAckContext<T> context = new BatchAckContext<>(message, msg, channel, deliveryTag);
            publishToDisruptor(context, properties, msg, deliveryTag);
        } catch (Exception e) {
            handleConsumeException(properties, channel, deliveryTag, msg, e);
        }
    }

    /**
     * 处理死信消息
     */
    protected void handleDeadLetterMessage(long deliveryTag, T message, Exception exception) {
        log.error("消息进入死信队列 [投递标签: {}] [异常: {}]", deliveryTag, exception.getMessage());
    }

    /**
     * 判断消息是否有效
     * 子类可以重写此方法进行自定义的消息有效性验证
     */
    protected boolean isValidMessage(Message message, T msg) {
        return msg != null;
    }
    
    /**
     * 获取最小批处理阈值
     */
    protected int getMinBatchSize() {
        return DEFAULT_MIN_BATCH_SIZE;
    }
    
    /**
     * 批量处理失败时是否应该重试
     */
    protected boolean shouldRetryOnBatchFailure(Exception e) {
        return true;
    }

    protected String getDisruptorEventType() {
        return "rabbitmq-batch";
    }

    protected String getDisruptorBusinessName() {
        return this.getClass().getSimpleName();
    }

    protected int getDisruptorRingBufferSize() {
        return 8192;
    }

    protected int getDisruptorConsumerThreads() {
        return 1;
    }

    protected int getDisruptorBatchSize() {
        return Math.max(getMinBatchSize(), 10);
    }

    protected long getDisruptorBatchTimeoutMillis() {
        return 200L;
    }

    protected String getDisruptorWaitStrategy() {
        return DisruptorWaitStrategy.BLOCKING;
    }

    protected DisruptorTemplate<BatchAckContext<T>> getOrCreateDisruptorTemplate() {
        if (disruptorTemplate != null) {
            return disruptorTemplate;
        }
        synchronized (this) {
            if (disruptorTemplate == null) {
                DisruptorTemplate<BatchAckContext<T>> template = DisruptorTemplate.<BatchAckContext<T>>builder()
                        .businessName(getDisruptorBusinessName())
                        .ringBufferSize(getDisruptorRingBufferSize())
                        .consumerThreads(getDisruptorConsumerThreads())
                        .waitStrategy(getDisruptorWaitStrategy())
                        .batchEnabled(true)
                        .batchSize(getDisruptorBatchSize())
                        .batchTimeout(getDisruptorBatchTimeoutMillis())
                        .batchEventProcessor(new RabbitmqBatchEventProcessor())
                        .build();
                template.start();
                disruptorTemplate = template;
            }
        }
        return disruptorTemplate;
    }

    private void publishToDisruptor(BatchAckContext<T> context, MessageProperties properties, T msg, long deliveryTag) {
        boolean published = getOrCreateDisruptorTemplate().publish(getDisruptorEventType(), context);
        if (published) {
            return;
        }
        int currentRetry = incrementRetryCount(properties);
        boolean requeue = shouldRetryOnException(new RuntimeException("Disruptor publish failed"))
                && currentRetry <= getMaxRetryCount();
        enqueueNack(context.getChannel(), deliveryTag, requeue);
        if (!requeue) {
            handleDeadLetterMessage(deliveryTag, msg, new RuntimeException("Disruptor publish failed"));
        }
    }

    private void handleConsumeException(MessageProperties properties, Channel channel, long deliveryTag, T msg, Exception e) {
        int currentRetry = incrementRetryCount(properties);
        boolean requeue = shouldRetryOnException(e) && currentRetry <= getMaxRetryCount();
        enqueueNack(channel, deliveryTag, requeue);
        if (!requeue) {
            handleDeadLetterMessage(deliveryTag, msg, e);
        }
    }

    protected void enqueueAck(Channel channel, long deliveryTag) {
        ensureAckExecutor(channel).execute(() -> {
            try {
                ackMessage(channel, deliveryTag);
            } catch (IOException e) {
                log.error("Ack失败 [投递标签: {}]", deliveryTag, e);
            }
        });
    }

    protected void enqueueNack(Channel channel, long deliveryTag, boolean requeue) {
        ensureAckExecutor(channel).execute(() -> {
            try {
                nackMessage(channel, deliveryTag, requeue);
            } catch (IOException e) {
                log.error("Nack失败 [投递标签: {}] [重新入队: {}]", deliveryTag, requeue, e);
            }
        });
    }

    protected ExecutorService ensureAckExecutor(Channel channel) {
        return ackExecutors.computeIfAbsent(channel, ch -> {
            ThreadFactory factory = runnable -> {
                Thread thread = new Thread(runnable, getDisruptorBusinessName() + "-rabbitmq-ack-" + ch.hashCode());
                thread.setDaemon(true);
                return thread;
            };
            ExecutorService executor = new ThreadPoolExecutor(
                    1,
                    1,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    factory
            );
            ch.addShutdownListener(cause -> shutdownAckExecutor(ch));
            return executor;
        });
    }

    protected void shutdownAckExecutor(Channel channel) {
        ExecutorService executor = ackExecutors.remove(channel);
        if (executor != null) {
            executor.shutdown();
        }
    }

    private class RabbitmqBatchEventProcessor implements BatchEventProcessor<BatchAckContext<T>> {
        @Override
        public ProcessResult processBatch(EventBatch<BatchAckContext<T>> batch) {
            List<Event<BatchAckContext<T>>> events = batch.getEvents();
            if (CollUtil.isEmpty(events)) {
                return ProcessResult.success("empty");
            }

            BatchAggregation<T> aggregation = buildAggregation(events);

            try {
                BatchProcessResult<R> result = BatchOperationConsumerTemplate.this.processBatch(aggregation.payloads);
                if (result.isSuccess()) {
                    for (BatchAckContext<T> ctx : aggregation.contexts) {
                        enqueueAck(ctx.getChannel(), ctx.getDeliveryTag());
                    }
                    return ProcessResult.success("batch success");
                }

                Exception error = result.getError() != null
                        ? result.getError()
                        : new RuntimeException(result.getErrorMessage());
                try {
                    handleBatchFailure(aggregation.contexts, error);
                } catch (IOException ioException) {
                    log.error("批量失败处理异常", ioException);
                }
                return ProcessResult.failure(result.getErrorMessage(), error);
            } catch (Exception e) {
                try {
                    handleBatchException(aggregation.contexts, e);
                } catch (IOException ioException) {
                    log.error("批量异常处理失败", ioException);
                }
                return ProcessResult.failure("batch exception", e);
            }
        }
    }

    private void handleBatchFailure(List<BatchAckContext<T>> contexts, Exception error) throws IOException {
        for (BatchAckContext<T> ctx : contexts) {
            MessageProperties properties = ctx.getMessage().getMessageProperties();
            int currentRetry = incrementRetryCount(properties);
            boolean shouldRequeue = shouldRetryOnBatchFailure(error) && currentRetry <= getMaxRetryCount();
            enqueueNack(ctx.getChannel(), ctx.getDeliveryTag(), shouldRequeue);
            if (!shouldRequeue) {
                handleDeadLetterMessage(ctx.getDeliveryTag(), ctx.getMsg(), error);
            }
        }
    }

    private void handleBatchException(List<BatchAckContext<T>> contexts, Exception error) throws IOException {
        for (BatchAckContext<T> ctx : contexts) {
            MessageProperties properties = ctx.getMessage().getMessageProperties();
            int currentRetry = incrementRetryCount(properties);
            boolean shouldRequeue = shouldRetryOnException(error) && currentRetry <= getMaxRetryCount();
            enqueueNack(ctx.getChannel(), ctx.getDeliveryTag(), shouldRequeue);
            if (!shouldRequeue) {
                handleDeadLetterMessage(ctx.getDeliveryTag(), ctx.getMsg(), error);
            }
        }
    }

    private BatchAggregation<T> buildAggregation(List<Event<BatchAckContext<T>>> events) {
        List<BatchAckContext<T>> contexts = new ArrayList<>(events.size());
        List<T> payloads = new ArrayList<>(events.size());
        for (Event<BatchAckContext<T>> event : events) {
            BatchAckContext<T> ctx = event.getPayload();
            if (ctx == null) {
                continue;
            }
            contexts.add(ctx);
            payloads.add(ctx.getMsg());
        }
        return new BatchAggregation<>(contexts, payloads);
    }
    
    /**
     * 批量处理消息
     * 核心批量业务处理逻辑，如批量入库等
     *
     * @param validMsgList 有效的业务消息列表
     * @return 批处理结果
     */
    protected abstract BatchProcessResult<R> processBatch(List<T> validMsgList);
    
    private static class BatchAggregation<T> {
        private final List<BatchAckContext<T>> contexts;
        private final List<T> payloads;

        private BatchAggregation(List<BatchAckContext<T>> contexts, List<T> payloads) {
            this.contexts = contexts;
            this.payloads = payloads;
        }
    }

    @Data
    protected static class BatchAckContext<T> {
        private final Message message;
        private final T msg;
        private final Channel channel;
        private final long deliveryTag;
    }
    
    /**
     * 批处理结果
     */
    @Data
    public static class BatchProcessResult<R> {
        // 处理结果
        private R result;
        // 是否成功
        private boolean success;
        // 错误信息
        private String errorMessage;
        // 错误异常
        private Exception error;
        
        public static <R> BatchProcessResult<R> success(R result) {
            BatchProcessResult<R> batchResult = new BatchProcessResult<>();
            batchResult.setResult(result);
            batchResult.setSuccess(true);
            return batchResult;
        }
        
        public static <R> BatchProcessResult<R> failure(String errorMessage) {
            BatchProcessResult<R> batchResult = new BatchProcessResult<>();
            batchResult.setSuccess(false);
            batchResult.setErrorMessage(errorMessage);
            return batchResult;
        }
        
        public static <R> BatchProcessResult<R> failure(String errorMessage, Exception error) {
            BatchProcessResult<R> batchResult = new BatchProcessResult<>();
            batchResult.setSuccess(false);
            batchResult.setErrorMessage(errorMessage);
            batchResult.setError(error);
            return batchResult;
        }
    }
} 
