package com.ww.app.im.enums;

import lombok.Getter;

/**
 * @author ww
 * @create 2024-12-25 21:21
 * @description:
 */
@Getter
public enum ImMsgBizCodeEnum {

    CHAT_MSG_BIZ(1001,"im聊天消息"),
    SEND_GIFT_SUCCESS(1002,"送礼成功"),
    SEND_GIFT_FAIL(1003,"送礼失败"),
    PK_SEND_GIFT_SUCCESS(1004,"pk送礼成功"),
    PK_ONLINE(1005,"pk连线");

    final int code;
    final String desc;

    ImMsgBizCodeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}
