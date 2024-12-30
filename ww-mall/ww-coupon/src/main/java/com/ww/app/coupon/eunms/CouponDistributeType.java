package com.ww.app.coupon.eunms;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-25- 09:41
 * @description:
 */
@Getter
public enum CouponDistributeType {

    RECEIVE("用户领取"),
    DISTRIBUTE("后台发放");

    private final String text;

    CouponDistributeType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
