package com.ww.app.disruptor.processor;

import com.ww.app.disruptor.model.Event;
import com.ww.app.disruptor.model.ProcessResult;

/**
 * 事件处理器接口 - 所有事件处理器的基础接口
 *
 * @author ww-framework
 */
@FunctionalInterface
public interface EventProcessor<T> {

    /**
     * 处理单个事件
     *
     * @param event 待处理的事件
     * @return 处理结果
     */
    ProcessResult process(Event<T> event);

    /**
     * 获取处理器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 处理器是否支持该事件类型
     *
     * @param eventType 事件类型
     * @return true表示支持
     */
    default boolean supports(String eventType) {
        return true;
    }

    /**
     * 处理器优先级（数值越大优先级越高）
     */
    default int getPriority() {
        return 0;
    }
}
