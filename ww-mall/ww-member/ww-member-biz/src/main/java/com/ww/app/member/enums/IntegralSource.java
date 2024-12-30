package com.ww.app.member.enums;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-21- 15:45
 * @description: 积分来源类型
 */
@Getter
public enum IntegralSource {

    /**
     * 抽奖
     */
    LUCKY_DRAW("抽奖"),

    /**
     * 注册
     */
    REGISTER("注册"),

    /**
     * 签到
     */
    SIGN("签到"),

    /**
     * 下单支付
     */
    ORDER_PAYMENT("下单支付"),

    /**
     * 取消支付
     */
    CANCEL_PAYMENT("取消支付"),

    /**
     * 售后退款
     */
    AFTER_SALE_REFUND("售后退款");

    private final String text;

    IntegralSource(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
