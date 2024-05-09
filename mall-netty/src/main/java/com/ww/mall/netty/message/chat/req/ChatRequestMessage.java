package com.ww.mall.netty.message.chat.req;

import com.ww.mall.netty.message.chat.MallChatMessage;
import lombok.*;

import static com.ww.mall.netty.holder.MessageTypeHolder.CHAT_REQUEST_MESSAGE_TYPE;

/**
 * @author ww
 * @create 2024-05-07 19:50
 * @description: 发送聊天信息请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ChatRequestMessage extends MallChatMessage {
    /**
     * 聊天内容
     */
    private String content;

    /**
     * 消息发送于
     */
    private String to;

    /**
     * 消息来自于
     */
    private String from;

    @Override
    public int getMessageType() {
        return CHAT_REQUEST_MESSAGE_TYPE;
    }
}
