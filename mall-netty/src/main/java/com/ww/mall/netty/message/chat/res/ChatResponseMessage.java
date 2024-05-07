package com.ww.mall.netty.message.chat.res;

import com.ww.mall.netty.message.AbstractResponseChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author ww
 * @create 2024-05-07 19:54
 * @description: 发送聊天请求响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ChatResponseMessage extends AbstractResponseChatMessage {

    /**
     * 消息来自于
     */
    private String from;

    /**
     * 消息内容
     */
    private String content;

    public ChatResponseMessage(boolean success, String reason) {
        super(success, reason);
    }

    @Override
    public int getMessageType() {
        return CHAT_RESPONSE_MESSAGE_TYPE;
    }
}
