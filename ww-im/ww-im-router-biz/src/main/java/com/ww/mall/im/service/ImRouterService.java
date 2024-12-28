package com.ww.mall.im.service;

import com.ww.mall.im.common.ImMsgBody;

import java.util.List;

/**
 * @author ww
 * @create 2024-12-24 17:27
 * @description:
 */
public interface ImRouterService {

    /**
     * 发送消息
     */
    boolean sendMsg(ImMsgBody imMsgBody);

    /**
     * 批量发送消息
     */
    void batchSendMsg(List<ImMsgBody> imMsgBodyList);

}
