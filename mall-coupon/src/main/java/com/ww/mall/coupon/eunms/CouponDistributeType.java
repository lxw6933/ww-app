package com.ww.mall.coupon.eunms;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-25- 09:41
 * @description:
 */
public enum CouponDistributeType {

    RECEIVE("用户领取"),
    DISTRIBUTE("后台发放");

    private String text;

    CouponDistributeType(String text) {
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
