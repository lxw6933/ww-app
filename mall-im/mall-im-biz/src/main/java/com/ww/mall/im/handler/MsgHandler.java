package com.ww.mall.im.handler;

import com.ww.mall.im.common.ImMsgBody;

/**
 * @author ww
 * @create 2024-12-25 21:29
 * @description:
 */
public interface MsgHandler {

    void handle(ImMsgBody imMsgBody);

    boolean supports(ImMsgBody imMsgBody);

}
