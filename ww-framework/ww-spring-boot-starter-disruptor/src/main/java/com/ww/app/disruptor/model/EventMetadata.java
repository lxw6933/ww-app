package com.ww.app.disruptor.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 事件元数据 - 存储事件的额外信息
 *
 * @author ww-framework
 */
@Data
public class EventMetadata implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件来源
     */
    private String source;

    /**
     * 事件优先级
     */
    private EventPriority priority;

    /**
     * 事件分区键（用于分区路由）
     */
    private String partitionKey;

    /**
     * 跟踪ID（用于链路追踪）
     */
    private String traceId;

    /**
     * 跨度ID（用于链路追踪）
     */
    private String spanId;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 开始处理时间
     */
    private LocalDateTime processStartTime;

    /**
     * 处理完成时间
     */
    private LocalDateTime processEndTime;

    /**
     * 处理耗时（毫秒）
     */
    private Long processDuration;

    /**
     * 标签（用于分类和过滤）
     */
    private String tags;

    public EventMetadata() {
        this.priority = EventPriority.NORMAL;
    }

    /**
     * 计算处理耗时
     */
    public void calculateDuration() {
        if (processStartTime != null && processEndTime != null) {
            this.processDuration = java.time.Duration.between(processStartTime, processEndTime).toMillis();
        }
    }

}
