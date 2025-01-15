package com.ww.app.im.core.api.enums;

import lombok.Getter;

/**
 * @author ww
 * @create 2024-12-24 20:11
 * @description:
 */
@Getter
public enum ImAppIdEnum {

    LIVE_BIZ(10001,"直播业务");

    final int code;
    final String desc;

    ImAppIdEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}
