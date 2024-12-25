package com.ww.mall.im.rpc;

import com.ww.mall.common.common.Result;
import com.ww.mall.im.common.ImMsgBody;
import com.ww.mall.im.fallback.ImMsgRouterApiFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * @author ww
 * @create 2024-12-24 21:29
 * @description:
 */
@RequestMapping("/mall-im-core/im/inner")
@FeignClient(value = "mall-im-core", fallbackFactory = ImMsgRouterApiFallback.class)
public interface ImMsgRouterApi {

    @PostMapping("/sendMsg")
    Result<Boolean> sendMsg(@RequestBody ImMsgBody imMsgBody);

    @PostMapping("/batchSendMsg")
    Result<Boolean> batchSendMsg(@RequestBody List<ImMsgBody> imMsgBodyList);

}
