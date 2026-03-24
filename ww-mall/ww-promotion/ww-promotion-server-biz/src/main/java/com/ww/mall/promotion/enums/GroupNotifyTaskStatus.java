package com.ww.mall.promotion.enums;

/**
 * 拼团通知任务状态枚举。
 *
 * @author ww
 * @create 2026-03-24
 * @description: 用于标识拼团业务 MQ 通知任务的生命周期状态
 */
public enum GroupNotifyTaskStatus {

    /**
     * 任务已创建，等待发送。
     */
    INIT,

    /**
     * 某个实例已领取任务，正在发送。
     */
    SENDING,

    /**
     * 任务已发送成功。
     */
    SUCCESS,

    /**
     * 任务发送失败，等待重试。
     */
    FAILED,

    /**
     * 任务超过最大重试次数，进入人工介入状态。
     */
    DEAD
}
