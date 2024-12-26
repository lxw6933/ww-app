package com.ww.mall.im.rpc;

import com.ww.mall.im.common.ImMsgBody;
import com.ww.mall.im.router.api.rpc.ImRouterApi;
import com.ww.mall.im.service.ImRouterService;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author ww
 * @create 2024-12-26- 10:50
 * @description:
 */
@RestController
public class ImRouterApiRpc implements ImRouterApi {

    @Resource
    private ImRouterService imRouterService;

    @Override
    public boolean sendMsg(ImMsgBody imMsgBody) {
        return imRouterService.sendMsg(imMsgBody);
    }

    @Override
    public void batchSendMsg(List<ImMsgBody> imMsgBodyList) {
        imRouterService.batchSendMsg(imMsgBodyList);
    }
}
