package com.ww.app.order.enums;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-28- 14:32
 * @description:
 */
@Getter
public enum ProductType {

    VIRTUAL("虚拟"),
    PHYSICAL("实物");

    private final String text;

    ProductType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
