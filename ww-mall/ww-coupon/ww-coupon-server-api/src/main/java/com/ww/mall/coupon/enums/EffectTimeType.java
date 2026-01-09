package com.ww.mall.coupon.enums;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-25- 09:08
 * @description:
 */
@Getter
public enum EffectTimeType {

    FIXED("固定"),
    AFTER_RECEIVING("领取后");

    private final String text;

    EffectTimeType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
