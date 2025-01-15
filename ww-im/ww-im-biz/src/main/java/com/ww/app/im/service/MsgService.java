package com.ww.app.im.service;

import com.ww.app.im.core.api.common.ImMsgBody;

/**
 * @author ww
 * @create 2024-12-25 21:25
 * @description:
 */
public interface MsgService {

    /**
     * 处理im服务器投递过来的消息
     */
    void handleImMsg(ImMsgBody imMsgBody);

}
