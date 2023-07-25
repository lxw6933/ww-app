package com.ww.mall.coupon.eunms;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-25- 10:09
 * @description:
 */
public enum CouponStatus {

    TO_TAKE_EFFECT("待生效"),
    IN_EFFECT("生效中"),
    EXPIRED("已失效"),
    USED("已使用"),
    OCCUPIED("已占用");

    private String text;

    CouponStatus(String text) {
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
