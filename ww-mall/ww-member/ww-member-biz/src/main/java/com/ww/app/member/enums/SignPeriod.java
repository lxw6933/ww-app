package com.ww.app.member.enums;

import com.ww.app.common.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 签到周期枚举
 */
@Getter
@AllArgsConstructor
public enum SignPeriod implements BaseEnum {
    
    WEEKLY("weekly", "周签到"),
    
    MONTHLY("monthly", "月签到");
    
    private final String code;
    private final String desc;

    @Override
    public String getShowValue() {
        return this.desc;
    }
}