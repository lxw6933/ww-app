package com.ww.mall.netty.message.chat;

import static com.ww.mall.netty.holder.MessageTypeHolder.PONG_MESSAGE_TYPE;

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
