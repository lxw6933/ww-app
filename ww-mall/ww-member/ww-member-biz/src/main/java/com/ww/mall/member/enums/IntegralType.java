package com.ww.mall.member.enums;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-21- 15:49
 * @description:
 */
@Getter
public enum IntegralType {

    INCREASE("增加积分"),
    DECREASE("减少积分");

    private final String text;

    IntegralType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
