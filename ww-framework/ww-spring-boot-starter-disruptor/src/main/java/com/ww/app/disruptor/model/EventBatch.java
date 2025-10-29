package com.ww.app.disruptor.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 事件批次 - 用于批量处理事件
 *
 * @author ww-framework
 */
@Data
public class EventBatch<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 批次ID
     */
    private String batchId;

    /**
     * 批次中的事件列表
     */
    private List<Event<T>> events;

    /**
     * 批次大小
     */
    private int size;

    /**
     * 批次创建时间
     */
    private LocalDateTime createTime;

    /**
     * 批次状态
     */
    private BatchStatus status;

    public EventBatch() {
        this.events = new ArrayList<>();
        this.createTime = LocalDateTime.now();
        this.status = BatchStatus.PENDING;
        this.size = 0;
    }

    public EventBatch(String batchId) {
        this();
        this.batchId = batchId;
    }

    /**
     * 添加事件到批次
     */
    public void addEvent(Event<T> event) {
        this.events.add(event);
        this.size = this.events.size();
    }

    /**
     * 批量添加事件
     */
    public void addEvents(List<Event<T>> events) {
        this.events.addAll(events);
        this.size = this.events.size();
    }

    /**
     * 是否为空批次
     */
    public boolean isEmpty() {
        return this.events.isEmpty();
    }

    /**
     * 清空批次
     */
    public void clear() {
        this.events.clear();
        this.size = 0;
    }

    /**
     * 标记为处理中
     */
    public void markProcessing() {
        this.status = BatchStatus.PROCESSING;
    }

    /**
     * 标记为已完成
     */
    public void markCompleted() {
        this.status = BatchStatus.COMPLETED;
    }

    /**
     * 标记为失败
     */
    public void markFailed() {
        this.status = BatchStatus.FAILED;
    }

    public void setEvents(List<Event<T>> events) {
        this.events = events;
        this.size = events.size();
    }

}
