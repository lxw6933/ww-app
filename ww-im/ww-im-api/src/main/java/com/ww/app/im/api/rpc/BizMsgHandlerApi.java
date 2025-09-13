package com.ww.app.im.api.rpc;

import com.ww.app.im.api.constants.ApiConstants;
import com.ww.app.im.core.api.common.ImMsgBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author ww
 * @create 2024-12-26- 10:36
 * @description: 测试流程用
 */
@Tag(name = "RPC 服务 - IM 消息业务处理")
@FeignClient(value = ApiConstants.NAME)
public interface BizMsgHandlerApi {

    String PREFIX = ApiConstants.PREFIX + "/msg";

    @Operation(summary = "IM 消息业务处理")
    @PostMapping(PREFIX + "/handleImMsg")
    void handleImMsg(@RequestBody ImMsgBody imMsgBody);

}
