package com.ww.mall.netty.message.chat;

/**
 * @author ww
 * @create 2024-05-07 19:47
 * @description:
 */
public class PingChatMessage extends MallChatMessage {

    @Override
    public int getMessageType() {
        return PING_MESSAGE_TYPE;
    }
}
