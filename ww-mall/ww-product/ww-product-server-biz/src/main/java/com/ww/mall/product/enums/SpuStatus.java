package com.ww.mall.product.enums;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-28- 14:38
 * @description:
 */
@Getter
public enum SpuStatus {

    DOWN("下架"),
    UP("上架"),
    FREEZE("冻结");

    private final String text;

    SpuStatus(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
