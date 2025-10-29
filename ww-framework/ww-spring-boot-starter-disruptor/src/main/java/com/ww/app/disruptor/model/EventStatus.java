package com.ww.app.disruptor.model;

/**
 * 事件状态枚举
 * 
 * @author ww-framework
 */
public enum EventStatus {
    
    /**
     * 已创建
     */
    CREATED,
    
    /**
     * 处理中
     */
    PROCESSING,
    
    /**
     * 已完成
     */
    COMPLETED,
    
    /**
     * 失败
     */
    FAILED,
    
    /**
     * 已取消
     */
    CANCELLED,
    
    /**
     * 等待重试
     */
    RETRY_PENDING
}
