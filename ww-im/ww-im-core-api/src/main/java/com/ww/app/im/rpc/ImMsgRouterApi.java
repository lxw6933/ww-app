package com.ww.app.im.rpc;

import com.ww.app.common.common.Result;
import com.ww.app.im.common.ImMsgBody;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author ww
 * @create 2024-12-24 21:29
 * @description:
 */
@FeignClient(value = "ww-im-core")
public interface ImMsgRouterApi {

    @PostMapping("/ww-im-core/im/inner/sendMsg")
    Result<Boolean> sendMsg(@RequestBody ImMsgBody imMsgBody);

    @PostMapping("/ww-im-core/im/inner/batchSendMsg")
    Result<Boolean> batchSendMsg(@RequestBody List<ImMsgBody> imMsgBodyList);

}
