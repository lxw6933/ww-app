package com.ww.mall.coupon.eunms;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-25- 09:16
 * @description:
 */
public enum CouponLimitReceiveTimeType {

    FOREVER("永久"),
    MONTH("月"),
    WEEK("周"),
    DAY("天");

    private String text;

    CouponLimitReceiveTimeType(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    /**
     * 重写方便文档展示
     * @return
     */
    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
