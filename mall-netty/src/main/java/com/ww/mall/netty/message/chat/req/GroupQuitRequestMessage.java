package com.ww.mall.netty.message.chat.req;

import com.ww.mall.netty.message.chat.MallChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static com.ww.mall.netty.holder.MessageTypeHolder.GROUP_QUIT_REQUEST_MESSAGE_TYPE;

/**
 * @author ww
 * @create 2024-05-07 20:59
 * @description: 离开群组请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GroupQuitRequestMessage extends MallChatMessage {

    /**
     * 组名
     */
    private String groupName;

    /**
     * 用户名
     */
    private String username;

    @Override
    public int getMessageType() {
        return GROUP_QUIT_REQUEST_MESSAGE_TYPE;
    }
}
