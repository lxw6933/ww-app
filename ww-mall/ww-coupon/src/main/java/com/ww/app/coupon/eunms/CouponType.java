package com.ww.app.coupon.eunms;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-25- 09:24
 * @description:
 */
@Getter
public enum CouponType {

    PLATFORM("平台"),
    MERCHANT("商家");

    private final String text;

    CouponType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
