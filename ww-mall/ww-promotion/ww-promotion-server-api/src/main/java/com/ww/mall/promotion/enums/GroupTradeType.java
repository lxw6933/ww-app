package com.ww.mall.promotion.enums;

import lombok.Getter;

/**
 * 拼团支付回调业务类型。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 用于区分支付回调是“开团”还是“参团”
 */
@Getter
public enum GroupTradeType {

    /**
     * 开团。
     */
    START,

    /**
     * 参团。
     */
    JOIN
}
