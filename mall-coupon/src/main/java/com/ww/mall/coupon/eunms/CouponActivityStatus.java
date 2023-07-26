package com.ww.mall.coupon.eunms;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-26- 13:45
 * @description:
 */
public enum CouponActivityStatus {

    TO_TAKE_EFFECT("待生效"),
    IN_EFFECT("生效中"),
    DUE_SOON("快到期"),
    EXPIRED("已失效");

    private String text;

    CouponActivityStatus(String text) {
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
