package com.ww.mall.im.handler;

import com.ww.mall.im.common.ImMsgBody;
import com.ww.mall.im.enums.ImMsgBizCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-12-25 21:30
 * @description: 聊天消息处理器
 */
@Slf4j
@Component
public class ChatMsgHandler implements MsgHandler {

    @Override
    public void handle(ImMsgBody imMsgBody) {

    }

    @Override
    public boolean supports(ImMsgBody imMsgBody) {
        return imMsgBody.getBizCode() == ImMsgBizCodeEnum.CHAT_MSG_BIZ.getCode();
    }
}
