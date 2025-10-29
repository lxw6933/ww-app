package com.ww.app.disruptor.processor;

import com.ww.app.disruptor.model.Event;
import com.ww.app.disruptor.model.EventBatch;
import com.ww.app.disruptor.model.ProcessResult;

import java.util.List;

/**
 * 批量事件处理器接口
 *
 * @author ww-framework
 */
public interface BatchEventProcessor<T> {

    /**
     * 批量处理事件
     *
     * @param batch 事件批次
     * @return 处理结果
     */
    ProcessResult processBatch(EventBatch<T> batch);

    /**
     * 批量处理事件列表
     *
     * @param events 事件列表
     * @return 处理结果
     */
    default ProcessResult processBatch(List<Event<T>> events) {
        EventBatch<T> batch = new EventBatch<>();
        batch.addEvents(events);
        return processBatch(batch);
    }

    /**
     * 获取批处理器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 获取批处理大小
     */
    default int getBatchSize() {
        return 100;
    }

    /**
     * 获取批处理超时时间（毫秒）
     */
    default long getBatchTimeout() {
        return 1000L;
    }
}
