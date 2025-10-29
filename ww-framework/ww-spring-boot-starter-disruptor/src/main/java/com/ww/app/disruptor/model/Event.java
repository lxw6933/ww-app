package com.ww.app.disruptor.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 事件基类 - 所有事件的基础模型
 *
 * @author ww-framework
 */
@Data
public class Event<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件唯一标识
     */
    private String eventId;

    /**
     * 事件类型/主题
     */
    private String eventType;

    /**
     * 事件数据负载
     */
    private T payload;

    /**
     * 事件元数据
     */
    private EventMetadata metadata;

    /**
     * 事件上下文（用于传递额外信息）
     */
    private Map<String, Object> context;

    /**
     * 事件创建时间
     */
    private LocalDateTime createTime;

    /**
     * 事件状态
     */
    private EventStatus status;

    /**
     * 重试次数
     */
    private int retryCount;

    /**
     * 最大重试次数
     */
    private int maxRetries;

    public Event() {
        this.eventId = UUID.randomUUID().toString();
        this.createTime = LocalDateTime.now();
        this.status = EventStatus.CREATED;
        this.context = new HashMap<>();
        this.retryCount = 0;
        this.maxRetries = 3;
        this.metadata = new EventMetadata();
    }

    public Event(String eventType, T payload) {
        this();
        this.eventType = eventType;
        this.payload = payload;
    }

    public Event(String eventId, String eventType, T payload) {
        this(eventType, payload);
        this.eventId = eventId;
    }

    /**
     * 添加上下文信息
     */
    public Event<T> addContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }

    /**
     * 获取上下文信息
     */
    public Object getContext(String key) {
        return this.context.get(key);
    }

    /**
     * 增加重试次数
     */
    public void incrementRetry() {
        this.retryCount++;
    }

    /**
     * 是否可以重试
     */
    public boolean canRetry() {
        return this.retryCount < this.maxRetries;
    }

    /**
     * 标记为处理中
     */
    public void markProcessing() {
        this.status = EventStatus.PROCESSING;
    }

    /**
     * 标记为已完成
     */
    public void markCompleted() {
        this.status = EventStatus.COMPLETED;
    }

    /**
     * 标记为失败
     */
    public void markFailed() {
        this.status = EventStatus.FAILED;
    }

    // Getters and Setters

    @Override
    public String toString() {
        return "Event{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", status=" + status +
                ", retryCount=" + retryCount +
                ", createTime=" + createTime +
                '}';
    }
}
