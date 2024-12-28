package com.ww.mall.product.enums;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-28- 14:32
 * @description:
 */
public enum SpuType {

    /**
     * 虚拟
     */
    VIRTUAL("虚拟"),
    /**
     * 实物
     */
    PHYSICAL("实物");

    private String text;

    SpuType(String text) {
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
