package com.ww.mall.order.enums;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-08-11- 09:40
 * @description: 主订单类型
 */
@Getter
public enum OrderType {

    NORMAL("正常"),
    LOTTERY("抽奖"),
    EQUITY("权益领取"),
    CONVERT_CODE("兑换码"),
    PRESALE("定金预售"),
    FULL_PAY_PRESALE("全款预售");

    private final String text;

    OrderType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
