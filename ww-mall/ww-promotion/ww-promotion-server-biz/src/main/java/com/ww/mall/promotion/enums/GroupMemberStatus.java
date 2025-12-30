package com.ww.mall.promotion.enums;

import lombok.Getter;

/**
 * @author ww
 * @create 2025-12-08 19:00
 * @description: 拼团成员状态枚举
 */
@Getter
public enum GroupMemberStatus {

    /**
     * 正常
     */
    NORMAL(1, "正常"),

    /**
     * 已退出
     */
    EXITED(0, "已退出");

    private final Integer code;
    private final String desc;

    GroupMemberStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}




