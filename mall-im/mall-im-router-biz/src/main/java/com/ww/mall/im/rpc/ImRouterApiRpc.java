package com.ww.mall.im.rpc;

import com.ww.mall.im.common.ImMsgBody;
import com.ww.mall.im.router.api.rpc.ImRouterApi;
import com.ww.mall.im.service.ImRouterService;
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
    @PostMapping("/sendMsg")
    public void sendMsg(ImMsgBody imMsgBody) {
        imRouterService.sendMsg(imMsgBody);
    }

    @Override
    @PostMapping("/batchSendMsg")
    public void batchSendMsg(List<ImMsgBody> imMsgBodyList) {
        imRouterService.batchSendMsg(imMsgBodyList);
    }
}
