package com.ww.app.coupon.eunms;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-25- 09:37
 * @description:
 */
@Getter
public enum AllowMemberRangeType {

    ALL("全部用户"),
    SPECIFY("指定用户");

    private final String text;

    AllowMemberRangeType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
