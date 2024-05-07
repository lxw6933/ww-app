package com.ww.mall.netty.message.chat.req;

import com.ww.mall.netty.message.chat.MallChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * @author ww
 * @create 2024-05-07 20:59
 * @description: 创建组请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GroupCreateRequestMessage extends MallChatMessage {

    /**
     * 组名
     */
    private String groupName;

    /**
     * 组成员
     */
    private Set<String> members;

    @Override
    public int getMessageType() {
        return GROUP_CREATE_REQUEST_MESSAGE_TYPE;
    }
}
