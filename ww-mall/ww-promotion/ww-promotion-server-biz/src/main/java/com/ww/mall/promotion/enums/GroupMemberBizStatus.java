package com.ww.mall.promotion.enums;

import lombok.Getter;

/**
 * 拼团成员业务状态。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 细化成员在拼团域内的生命周期，便于客服回放与售后处理
 */
@Getter
public enum GroupMemberBizStatus {

    /**
     * 已支付并占位，等待成团。
     */
    JOINED,

    /**
     * 已成团。
     */
    SUCCESS,

    /**
     * 售后成功，名额已归还。
     */
    AFTER_SALE_RELEASED,

    /**
     * 团失败后待退款。
     */
    FAILED_REFUND_PENDING,

    /**
     * 团长售后导致团关闭。
     */
    LEADER_AFTER_SALE_CLOSED;
}
