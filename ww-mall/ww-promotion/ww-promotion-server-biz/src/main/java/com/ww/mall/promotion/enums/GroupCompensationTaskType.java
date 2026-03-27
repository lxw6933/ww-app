package com.ww.mall.promotion.enums;

/**
 * 拼团补偿任务类型。
 * <p>
 * 该枚举只描述“主状态已经落成，但后续副作用失败”的补偿场景，
 * 便于定时任务按类型执行对应的恢复动作。
 *
 * @author ww
 * @create 2026-03-27
 * @description: 拼团补偿任务类型
 */
public enum GroupCompensationTaskType {

    /**
     * Mongo 投影补偿。
     */
    PROJECTION_SYNC,

    /**
     * 退款补偿消息重发。
     */
    REFUND_RETRY
}
