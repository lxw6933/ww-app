package com.ww.mall.coupon.eunms;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2024-03-12- 15:04
 * @description:
 */
public enum CodeLength {

    EIGHT(8,"8位"),
    TEN(10,"10位"),
    TWELVE(12,"12位"),
    SIXTEEN(16,"16位");

    CodeLength(Integer code, String text) {
        this.code = code;
        this.text = text;
    }

    private Integer code;

    private String text;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
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
