package com.ww.mall.promotion.enums;

import lombok.Getter;

/**
 * 拼团链路关键阶段。
 * <p>
 * `persistOnNonFailure` 用于控制“成功/处理中/跳过”是否需要落库：
 * 1. 跨边界、可用于业务审计的关键检查点保留。
 * 2. 高频内部细节成功日志只打印，不写 Mongo，降低重复度与存储压力。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 统一拼团链路阶段枚举
 */
@Getter
public enum GroupFlowStage {

    /**
     * 支付回调入口。
     */
    PAY_CALLBACK(true),

    /**
     * 支付成功消息消费。
     */
    PAY_ORDER_MQ(true),

    /**
     * 售后成功消息消费。
     */
    AFTER_SALE_MQ(true),

    /**
     * 开团请求。
     */
    CREATE_GROUP(true),

    /**
     * 参团请求。
     */
    JOIN_GROUP(true),

    /**
     * 成团状态流转。
     */
    GROUP_SUCCESS(true),

    /**
     * 失败状态流转。
     */
    GROUP_FAILED(true),

    /**
     * 团内成员售后释放名额。
     */
    MEMBER_RELEASE(true),

    /**
     * 拼团过期关闭。
     */
    GROUP_EXPIRE(true),

    /**
     * 成团通知 MQ。
     */
    GROUP_SUCCESS_MQ(true),

    /**
     * 失败通知 MQ。
     */
    GROUP_FAILED_MQ(true),

    /**
     * 退款通知 MQ。
     */
    GROUP_REFUND_MQ(true),

    /**
     * 保存实例。
     */
    SAVE_INSTANCE(false),

    /**
     * 保存成员。
     */
    SAVE_MEMBER(false),

    /**
     * 批量保存实例。
     */
    SAVE_INSTANCE_BULK(false),

    /**
     * 批量保存成员。
     */
    SAVE_MEMBER_BULK(false),

    /**
     * 同步快照。
     */
    SYNC_GROUP_SNAPSHOT(false),

    /**
     * 未知事件。
     */
    UNKNOWN_EVENT(true);

    /**
     * 非失败状态是否需要持久化。
     */
    private final boolean persistOnNonFailure;

    GroupFlowStage(boolean persistOnNonFailure) {
        this.persistOnNonFailure = persistOnNonFailure;
    }
}
