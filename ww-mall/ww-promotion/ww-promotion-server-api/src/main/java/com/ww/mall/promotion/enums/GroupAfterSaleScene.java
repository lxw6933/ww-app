package com.ww.mall.promotion.enums;

import lombok.Getter;

/**
 * 拼团售后处理场景。
 *
 * @author ww
 * @create 2026-03-27
 * @description: 用于区分需要执行拼团售后脚本的 OPEN 售后，与仅需退款的支付后入团异常场景
 */
@Getter
public enum GroupAfterSaleScene {

    /**
     * OPEN 拼团中的订单发起售后。
     */
    OPEN_APPLY,

    /**
     * 支付后创建团/参团被拒绝，仅需触发退款。
     */
    TRADE_EXCEPTION_REFUND
}
