package com.ww.app.im.service;

import com.ww.app.im.core.api.common.ImMsgBody;
import com.ww.app.im.core.api.enums.ImMsgCodeEnum;

/**
 * @author ww
 * @create 2024-12-24 17:39
 * @description:
 */
public interface MsgRouterService {

    /**
     * 接收需要发送的消息
     */
    void onReceive(ImMsgBody imMsgBody);


    /**
     * 发送消息给客户端
     */
    boolean sendMsgToClient(ImMsgBody imMsgBody);

    boolean sendMsgToClient(ImMsgCodeEnum msgCodeEnum, ImMsgBody imMsgBody);
}
