package com.ww.mall.im.rpc;

import com.ww.mall.im.common.ImMsgBody;
import com.ww.mall.im.service.MsgService;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-12-26- 10:42
 * @description:
 */
@RestController
public class BizMsgHandlerApiRpc implements BizMsgHandlerApi {

    @Resource
    private MsgService msgService;

    @Override
    public void handleImMsg(ImMsgBody imMsgBody) {
        msgService.handleImMsg(imMsgBody);
    }
}
