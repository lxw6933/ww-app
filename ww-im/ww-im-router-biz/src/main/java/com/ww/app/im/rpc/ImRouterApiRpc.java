package com.ww.app.im.rpc;

import com.ww.app.im.core.api.common.ImMsgBody;
import com.ww.app.im.router.api.rpc.ImRouterApi;
import com.ww.app.im.service.ImRouterService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author ww
 * @create 2024-12-26- 10:50
 * @description:
 */
@RestController
@RequestMapping("/im/inner")
public class ImRouterApiRpc implements ImRouterApi {

    @Resource
    private ImRouterService imRouterService;

    @Override
    @PostMapping("/routeMsg")
    public void routeMsg(ImMsgBody imMsgBody) {
        imRouterService.sendMsg(imMsgBody);
    }

    @Override
    @PostMapping("/batchRouteMsg")
    public void batchRouteMsg(List<ImMsgBody> imMsgBodyList) {
        imRouterService.batchSendMsg(imMsgBodyList);
    }
}
