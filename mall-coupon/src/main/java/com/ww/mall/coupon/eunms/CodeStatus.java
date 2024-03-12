package com.ww.mall.coupon.eunms;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2024-03-12- 14:17
 * @description:
 */
public enum CodeStatus {

    UNUSED("未核销"),
    USING("核销中"),
    CONSUMED("已核销"),
    DISCARD("已作废"),
    OVERDUE("已过期");

    CodeStatus(String text) {
        this.text = text;
    }

    private String text;

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
