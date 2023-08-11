package com.ww.mall.order.enums;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-08-11- 13:36
 * @description:
 */
public enum RechargeAccountType {

    NULL("无"),
    QQ("QQ"),
    TEL("手机号"),
    WX("微信");

    private String text;

    RechargeAccountType(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
