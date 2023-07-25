package com.ww.mall.coupon.eunms;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-25- 09:37
 * @description:
 */
public enum AllowMemberRangeType {

    ALL("全部用户"),
    SPECIFY("指定用户");

    private String text;

    AllowMemberRangeType(String text) {
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
