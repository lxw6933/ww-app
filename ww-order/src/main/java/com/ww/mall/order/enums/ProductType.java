package com.ww.mall.order.enums;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-28- 14:32
 * @description:
 */
public enum ProductType {

    VIRTUAL("虚拟"),
    PHYSICAL("实物");

    private String text;

    ProductType(String text) {
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
