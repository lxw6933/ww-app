package com.ww.mall.netty.message.chat.req;

import com.ww.mall.netty.message.chat.MallChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author ww
 * @create 2024-05-07 20:59
 * @description: 登陆请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LoginRequestMessage extends MallChatMessage {

    /**
     * 账号
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    @Override
    public int getMessageType() {
        return LOGIN_REQUEST_MESSAGE_TYPE;
    }
}
