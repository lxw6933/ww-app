package com.ww.mall.netty.message.chat.res;

import com.ww.mall.netty.message.AbstractResponseChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static com.ww.mall.netty.holder.MessageTypeHolder.GROUP_CHAT_RESPONSE_MESSAGE_TYPE;

/**
 * @author ww
 * @create 2024-05-07 19:54
 * @description: 群组聊天请求响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GroupChatResponseMessage extends AbstractResponseChatMessage {

    /**
     * 消息来自于
     */
    private String from;

    /**
     * 消息内容
     */
    private String content;

    public GroupChatResponseMessage(boolean success, String reason) {
        super(success, reason);
    }

    @Override
    public int getMessageType() {
        return GROUP_CHAT_RESPONSE_MESSAGE_TYPE;
    }
}
