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

}
