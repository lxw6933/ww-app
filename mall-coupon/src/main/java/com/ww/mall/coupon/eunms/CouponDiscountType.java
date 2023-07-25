package com.ww.mall.coupon.eunms;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-25- 09:23
 * @description:
 */
public enum CouponDiscountType {

    FULL_REDUCTION("满减券"),
    DIRECT_REDUCTION("代金券"),
    FULL_DISCOUNT("折扣券");

    private String text;

    CouponDiscountType(String text) {
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
