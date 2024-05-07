package com.ww.mall.netty.message.chat.req;

import com.ww.mall.netty.message.chat.MallChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author ww
 * @create 2024-05-07 19:56
 * @description: 发送群聊消息请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GroupChatRequestMessage extends MallChatMessage {

    /**
     * 消息内容
     */
    private String content;

    /**
     * 组名称
     */
    private String groupName;

    /**
     * 消息来自于
     */
    private String from;

    @Override
    public int getMessageType() {
        return GROUP_CHAT_REQUEST_MESSAGE_TYPE;
    }
}
