package com.ww.mall.promotion.engine.model;

/**
 * 拼团领域事件类型。
 *
 * @author ww
 * @create 2026-03-19
 * @description: Redis Stream 中投递的拼团领域事件类型枚举
 */
public enum GroupDomainEventType {

    /**
     * 开团成功。
     */
    GROUP_CREATED,

    /**
     * 参团成功但尚未满团。
     */
    GROUP_JOINED,

    /**
     * 参团后达到门槛并成团。
     */
    GROUP_COMPLETED,

    /**
     * 售后释放单个成员名额。
     */
    GROUP_MEMBER_AFTER_SALE_RELEASED,

    /**
     * 售后仅记录轨迹，不改变团态。
     */
    GROUP_MEMBER_AFTER_SALE_AUDITED,

    /**
     * 拼团失败关闭。
     */
    GROUP_FAILED
}
