package com.ww.app.im.core.api.rpc;

import com.ww.app.common.common.Result;
import com.ww.app.im.core.api.common.ImMsgBody;
import com.ww.app.im.core.api.constants.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author ww
 * @create 2024-12-24 21:29
 * @description:
 */
@Tag(name = "RPC 服务 - IM 核心")
@FeignClient(value = ApiConstants.NAME)
public interface ImMsgRouterApi {

    String PREFIX = ApiConstants.PREFIX + "/im";

    @Operation(summary = "IM 发送消息")
    @PostMapping(PREFIX + "/sendMsg")
    Result<Boolean> sendMsg(@RequestBody ImMsgBody imMsgBody);

    @Operation(summary = "IM 批量发送消息")
    @PostMapping(PREFIX + "/batchSendMsg")
    Result<Boolean> batchSendMsg(@RequestBody List<ImMsgBody> imMsgBodyList);

}
