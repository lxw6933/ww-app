package com.ww.app.im.rpc;

import com.ww.app.common.common.Result;
import com.ww.app.im.common.ImMsgBody;
import com.ww.app.im.service.MsgRouterService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author ww
 * @create 2024-12-25 10:56
 * @description:
 */
@RestController
@RequestMapping("/im/inner")
public class ImMsgRouterApiRpc implements ImMsgRouterApi {

    @Resource
    private MsgRouterService msgRouterService;

    @Override
    @PostMapping("sendMsg")
    public Result<Boolean> sendMsg(ImMsgBody imMsgBody) {
        return Result.success(msgRouterService.sendMsgToClient(imMsgBody));
    }

    @Override
    @PostMapping("batchSendMsg")
    public Result<Boolean> batchSendMsg(List<ImMsgBody> imMsgBodyList) {
        imMsgBodyList.forEach(imMsgBody -> msgRouterService.sendMsgToClient(imMsgBody));
        return Result.success(true);
    }
}
