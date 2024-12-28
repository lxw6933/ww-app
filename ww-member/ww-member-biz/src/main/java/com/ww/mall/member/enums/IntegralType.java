package com.ww.mall.member.enums;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-21- 15:49
 * @description:
 */
public enum IntegralType {

    INCREASE("增加积分"),
    DECREASE("减少积分");

    private String text;

    IntegralType(String text) {
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
