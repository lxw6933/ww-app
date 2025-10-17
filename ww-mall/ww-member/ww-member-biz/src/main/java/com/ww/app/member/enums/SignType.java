package com.ww.app.member.enums;

import com.ww.app.common.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ww
 * @create 2025-10-15 16:11
 * @description:
 */
@Getter
@AllArgsConstructor
public enum SignType implements BaseEnum {

    WEEK("周签到表", 1),
    MONTH("月签到表", 2),
    ;

    private final String name;

    private final int type;

    @Override
    public String getShowValue() {
        return this.name;
    }
}
