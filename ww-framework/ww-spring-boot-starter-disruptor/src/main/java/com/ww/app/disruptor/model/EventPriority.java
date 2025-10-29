package com.ww.app.disruptor.model;

import lombok.Getter;

/**
 * 事件优先级枚举
 *
 * @author ww-framework
 */
@Getter
public enum EventPriority {

    /**
     * 低优先级
     */
    LOW(1),

    /**
     * 普通优先级
     */
    NORMAL(5),

    /**
     * 高优先级
     */
    HIGH(8),

    /**
     * 紧急优先级
     */
    URGENT(10);

    private final int level;

    EventPriority(int level) {
        this.level = level;
    }

}
