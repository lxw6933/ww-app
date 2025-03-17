package com.ww.mall.coupon.eunms;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-25- 09:41
 * @description:
 */
@Getter
public enum IssueType {

    RECEIVE("用户领取"),
    ADMIN_ISSUE("后台发放"),
    EXPORT_ISSUE("导出发放");

    private final String text;

    IssueType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
