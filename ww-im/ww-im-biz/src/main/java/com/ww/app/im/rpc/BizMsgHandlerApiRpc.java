package com.ww.app.im.rpc;

import com.ww.app.im.api.rpc.BizMsgHandlerApi;
import com.ww.app.im.core.api.common.ImMsgBody;
import com.ww.app.im.service.MsgService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-12-26- 10:42
 * @description:
 */
@RestController
@RequestMapping(BizMsgHandlerApi.PREFIX)
public class BizMsgHandlerApiRpc implements BizMsgHandlerApi {

    @Resource
    private MsgService msgService;

    @Override
    @PostMapping("/handleImMsg")
    public void handleImMsg(ImMsgBody imMsgBody) {
        msgService.handleImMsg(imMsgBody);
    }
}
