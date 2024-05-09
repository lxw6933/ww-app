package com.ww.mall.netty.message.chat;

import static com.ww.mall.netty.holder.MessageTypeHolder.HEARTBEAT_ACK_MESSAGE_TYPE;

/**
 * @author ww
 * @create 2024-05-09 22:48
 * @description:
 */
public class HeartbeatAckMessage extends MallChatMessage {
    @Override
    public int getMessageType() {
        return HEARTBEAT_ACK_MESSAGE_TYPE;
    }
}
