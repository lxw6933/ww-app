package com.ww.mall.product.enums;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-29- 09:23
 * @description:
 */
public enum CommentType {

    COMMENT("评论"),
    REPLAY("回复");

    private String text;

    CommentType(String text) {
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
