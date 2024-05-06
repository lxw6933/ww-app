package com.ww.mall.netty.enums;

import lombok.Getter;

@Getter
public enum WSMsgAction {

    CONNECT(1, "客户端初始化建立连接"),
    KEEPALIVE(2, "客户端保持心跳"),
    MESSAGE_SIGN(3, "客户端连接请求-服务端响应-消息签收"),
    BREAK_OFF(4, "服务端主动断开连接"),
    BUSINESS(5, "服务端主动推送业务消息"),
    SEND_TO_SOMEONE(9, "发送消息给某人(用于通信测试)");

    public final Integer type;

    public final String content;

    WSMsgAction(Integer type, String content) {
        this.type = type;
        this.content = content;
    }

}
