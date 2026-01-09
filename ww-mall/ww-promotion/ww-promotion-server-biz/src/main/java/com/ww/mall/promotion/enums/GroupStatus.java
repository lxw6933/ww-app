package com.ww.mall.promotion.enums;

import lombok.Getter;

/**
 * @author ww
 * @create 2025-12-08 19:00
 * @description: 拼团状态枚举
 */
@Getter
public enum GroupStatus {

    /**
     * 进行中
     */
    OPEN("OPEN", "进行中"),

    /**
     * 成功
     */
    SUCCESS("SUCCESS", "成功"),

    /**
     * 失败
     */
    FAILED("FAILED", "失败");

    private final String code;
    private final String desc;

    GroupStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}




