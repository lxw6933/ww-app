package com.ww.mall.netty.message.chat;

/**
 * @author ww
 * @create 2024-05-07 19:47
 * @description:
 */
public class PongChatMessage extends MallChatMessage {
    @Override
    public int getMessageType() {
        return PONG_MESSAGE_TYPE;
    }
}
