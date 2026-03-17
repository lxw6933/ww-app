package com.ww.mall.promotion.enums;

import lombok.Getter;

/**
 * 拼团支付回调处理状态。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 用于保证支付回调的处理链路可追踪与幂等
 */
@Getter
public enum GroupTradeStatus {

    /**
     * 待处理。
     */
    WAITING,

    /**
     * 处理中。
     */
    PROCESSING,

    /**
     * 处理成功。
     */
    SUCCESS,

    /**
     * 处理失败。
     */
    FAILED
}
