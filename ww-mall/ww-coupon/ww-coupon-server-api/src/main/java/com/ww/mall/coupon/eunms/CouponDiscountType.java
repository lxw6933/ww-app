package com.ww.mall.coupon.eunms;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-25- 09:23
 * @description:
 */
@Getter
public enum CouponDiscountType {

    FULL_REDUCTION("满减券"),
    DIRECT_REDUCTION("代金券"),
    FULL_DISCOUNT("折扣券"),
    INTEGRAL_DISCOUNT("积分券[满减]");

    private final String text;

    CouponDiscountType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
