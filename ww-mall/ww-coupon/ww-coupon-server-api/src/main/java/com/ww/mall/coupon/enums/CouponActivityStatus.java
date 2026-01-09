package com.ww.mall.coupon.enums;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-26- 13:45
 * @description:
 */
@Getter
public enum CouponActivityStatus {

    TO_TAKE_EFFECT("待生效"),
    IN_EFFECT("生效中"),
    DUE_SOON("快到期"),
    EXPIRED("已失效");

    private final String text;

    CouponActivityStatus(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
