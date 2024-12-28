package com.ww.mall.product.enums;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-29- 08:54
 * @description:
 */
public enum AttrType {

    BASE("基本属性"),
    SKU("销售属性");

    private String text;

    AttrType(String text) {
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
