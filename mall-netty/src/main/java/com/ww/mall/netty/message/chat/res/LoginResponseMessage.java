package com.ww.mall.netty.message.chat.res;

import com.ww.mall.netty.message.AbstractResponseChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static com.ww.mall.netty.holder.MessageTypeHolder.LOGIN_RESPONSE_MESSAGE_TYPE;

/**
 * @author ww
 * @create 2024-05-07 19:54
 * @description: 登陆请求响应
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LoginResponseMessage extends AbstractResponseChatMessage {

    public LoginResponseMessage(boolean success, String reason) {
        super(success, reason);
    }

    @Override
    public int getMessageType() {
        return LOGIN_RESPONSE_MESSAGE_TYPE;
    }
}
