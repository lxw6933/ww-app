package com.ww.mall.common.enums;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2024-03-12- 15:04
 * @description:
 */
@Getter
public enum CodeLength {

    EIGHT(8,"[8]"),
    TEN(10,"[10]"),
    TWELVE(12,"[12]"),
    SIXTEEN(16,"[16]");

    CodeLength(Integer code, String text) {
        this.code = code;
        this.text = text;
    }

    private final Integer code;

    private final String text;

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
