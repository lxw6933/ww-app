package com.ww.mall.netty.message.chat.req;

import com.ww.mall.netty.message.chat.MallChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author ww
 * @create 2024-05-07 20:59
 * @description: 获取群组成员信息请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GroupMembersRequestMessage extends MallChatMessage {

    /**
     * 组名
     */
    private String groupName;

    @Override
    public int getMessageType() {
        return GROUP_MEMBERS_REQUEST_MESSAGE_TYPE;
    }
}
