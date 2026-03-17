package com.ww.mall.promotion.enums;

import lombok.Getter;

/**
 * 拼团活动启用状态。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 统一启用/禁用状态码，避免直接写 0/1
 */
@Getter
public enum GroupEnabledStatus {

    /**
     * 禁用。
     */
    DISABLED(0),

    /**
     * 启用。
     */
    ENABLED(1);

    /**
     * 状态码。
     */
    private final int code;

    GroupEnabledStatus(int code) {
        this.code = code;
    }
}
