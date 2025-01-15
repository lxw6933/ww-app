package com.ww.app.im.handler;

import com.ww.app.im.core.api.common.ImMsgBody;

/**
 * @author ww
 * @create 2024-12-25 21:29
 * @description:
 */
public interface MsgHandler {

    void handle(ImMsgBody imMsgBody);

    boolean supports(ImMsgBody imMsgBody);

}
