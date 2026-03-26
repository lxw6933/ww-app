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
    DISABLED(false),

    /**
     * 启用。
     */
    ENABLED(true);

    /**
     * 启用状态值。
     */
    private final boolean enabled;

    GroupEnabledStatus(boolean enabled) {
        this.enabled = enabled;
    }
}
