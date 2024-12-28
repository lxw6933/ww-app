package com.ww.mall.order.enums;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-08-11- 14:11
 * @description:
 */
public enum OrderSourceType {

    COMMON("正常"),
    TOPIC("主题"),
    ARTICLE("文章");

    private String text;

    OrderSourceType(String text) {
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
