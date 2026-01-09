package com.ww.mall.coupon.enums;

import com.ww.app.common.enums.BaseEnum;
import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-25- 10:09
 * @description:
 */
@Getter
public enum CouponStatus implements BaseEnum {

    WAIT("待领取"),
    TO_TAKE_EFFECT("待生效"),
    IN_EFFECT("生效中"),
    EXPIRED("已失效"),
    USED("已使用"),
    OCCUPIED("已占用");

    private final String text;

    CouponStatus(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

    @Override
    public String getShowValue() {
        return this.text;
    }
}
