package com.ww.mall.promotion.enums;

import lombok.Getter;

/**
 * 拼团活动状态。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 统一拼团活动状态码，避免业务代码直接写 0/1/2/3
 */
@Getter
public enum GroupActivityStatus {

    /**
     * 未开始。
     */
    NOT_STARTED(0),

    /**
     * 进行中。
     */
    ACTIVE(1),

    /**
     * 已结束。
     */
    ENDED(2),

    /**
     * 已取消。
     */
    CANCELED(3);

    /**
     * 状态码。
     */
    private final int code;

    GroupActivityStatus(int code) {
        this.code = code;
    }
}
