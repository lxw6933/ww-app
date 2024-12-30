package com.ww.app.im.enums;

import lombok.Getter;

/**
 * @author ww
 * @create 2024-11-09 20:23
 * @description:
 */
@Getter
public enum ImMsgCodeEnum {

    IM_LOGIN_MSG(101,"登录消息"),
    IM_LOGOUT_MSG(102,"登出消息"),
    IM_HEARTBEAT_MSG(103,"心跳消息"),
    IM_BIZ_MSG(104,"业务消息"),
    IM_ACK_MSG(105,"ack消息");

    private final int code;

    private final String desc;

    ImMsgCodeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}
