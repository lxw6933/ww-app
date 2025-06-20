package com.ww.app.member.enums;

import lombok.Getter;

/**
 * 签到周期枚举
 */
@Getter
public enum SignPeriodEnum {
    
    /**
     * 周签到
     */
    WEEKLY("weekly", "周签到"),
    
    /**
     * 月签到
     */
    MONTHLY("monthly", "月签到");
    
    private final String code;
    private final String desc;
    
    SignPeriodEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}