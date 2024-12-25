package com.ww.mall.im.rpc;

import com.ww.mall.common.common.Result;
import com.ww.mall.im.common.ImMsgBody;
import com.ww.mall.im.service.MsgRouterService;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author ww
 * @create 2024-12-25 10:56
 * @description:
 */
@RestController
public class ImCoreRpc implements ImMsgRouterApi {

    @Resource
    private MsgRouterService msgRouterService;

    @Override
    public Result<Boolean> sendMsg(ImMsgBody imMsgBody) {
        return Result.success(msgRouterService.sendMsgToClient(imMsgBody));
    }

    @Override
    public Result<Boolean> batchSendMsg(List<ImMsgBody> imMsgBodyList) {
        imMsgBodyList.forEach(imMsgBody -> msgRouterService.sendMsgToClient(imMsgBody));
        return Result.success(true);
    }
}
