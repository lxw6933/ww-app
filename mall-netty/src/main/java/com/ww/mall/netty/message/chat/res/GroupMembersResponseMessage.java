package com.ww.mall.netty.message.chat.res;

import com.ww.mall.netty.message.AbstractResponseChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * @author ww
 * @create 2024-05-07 19:54
 * @description: 获取群组成员请求响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GroupMembersResponseMessage extends AbstractResponseChatMessage {

    /**
     * 组成员
     */
    private Set<String> members;

    public GroupMembersResponseMessage(boolean success, String reason) {
        super(success, reason);
    }

    @Override
    public int getMessageType() {
        return GROUP_MEMBERS_RESPONSE_MESSAGE_TYPE;
    }
}
