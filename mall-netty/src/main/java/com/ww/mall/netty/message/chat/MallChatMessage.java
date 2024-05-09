package com.ww.mall.netty.message.chat;

import com.ww.mall.netty.message.chat.req.*;
import com.ww.mall.netty.message.chat.res.*;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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
