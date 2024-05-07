package com.ww.mall.netty.message.chat.res;

import com.ww.mall.netty.message.AbstractResponseChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author ww
 * @create 2024-05-07 19:54
 * @description: 创建群组请求响应
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GroupCreateResponseMessage extends AbstractResponseChatMessage {

    public GroupCreateResponseMessage(boolean success, String reason) {
        super(success, reason);
    }

    @Override
    public int getMessageType() {
        return GROUP_CREATE_RESPONSE_MESSAGE_TYPE;
    }
}
