package com.ww.app.product.enums;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-29- 08:54
 * @description:
 */
@Getter
public enum AttrType {

    BASE("基本属性"),
    SKU("销售属性");

    private final String text;

    AttrType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
