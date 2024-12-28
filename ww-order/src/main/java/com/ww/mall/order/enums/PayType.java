package com.ww.mall.order.enums;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-08-11- 09:40
 * @description: 支付类型
 */
public enum PayType {

    CASH("现金【积分抵扣】"),
    STAGES("分期【积分抵扣】"),
    INTEGRAL("纯积分兑换"),
    CASH_INTEGRAL("固定积分+固定现金");

    private String text;

    PayType(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
