package com.ww.app.disruptor.model;

/**
 * 批次状态枚举
 *
 * @author ww-framework
 */
public enum BatchStatus {

    /**
     * 待处理
     */
    PENDING,

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
    FAILED
}
