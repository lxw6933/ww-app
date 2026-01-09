package com.ww.mall.coupon.enums;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-25- 09:16
 * @description:
 */
@Getter
public enum LimitReceiveTimeType {

    FOREVER("永久"),
    MONTH("月"),
    WEEK("周"),
    DAY("天");

    private final String text;

    LimitReceiveTimeType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
