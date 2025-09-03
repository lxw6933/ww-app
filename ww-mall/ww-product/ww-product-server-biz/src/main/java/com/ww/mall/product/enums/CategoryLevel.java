package com.ww.mall.product.enums;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-28- 15:08
 * @description:
 */
@Getter
public enum CategoryLevel {

    LEVEL_1("一级类目"),
    LEVEL_2("二级类目"),
    LEVEL_3("三级类目");

    private final String text;

    CategoryLevel(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
