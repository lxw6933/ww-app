package com.ww.mall.im.service;

import com.ww.mall.im.common.ImMsgBody;

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

}
