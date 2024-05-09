package com.ww.mall.netty.message.chat;

import lombok.Data;

import java.io.Serializable;

/**
 * @author ww
 * @create 2024-05-06 22:07
 * @description:
 */
@Data
public abstract class MallChatMessage implements Serializable {

    private int sequenceId;

    private int messageType;

    public abstract int getMessageType();

}
