package com.ww.app.common.enums;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2024-03-12- 14:17
 * @description:
 */
@Getter
public enum CodeStatus {

    UNUSED("未核销"),
    USING("核销中"),
    CONSUMED("已核销"),
    DISCARD("已作废"),
    OVERDUE("已过期");

    CodeStatus(String text) {
        this.text = text;
    }

    private final String text;

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
