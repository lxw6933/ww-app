package com.ww.app.im.router.api.rpc;

import com.ww.app.im.core.api.common.ImMsgBody;
import com.ww.app.im.router.api.constants.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author ww
 * @create 2024-12-26- 10:47
 * @description:
 */
@Tag(name = "RPC 服务 - IM 路由")
@FeignClient(value = ApiConstants.NAME)
public interface ImRouterApi {

    String PREFIX = ApiConstants.PREFIX + "/router";

    @Operation(summary = "IM 消息路由")
    @PostMapping(PREFIX + "/routeMsg")
    void routeMsg(@RequestBody ImMsgBody imMsgBody);

    @Operation(summary = "IM 批量消息路由")
    @PostMapping(PREFIX + "/batchRouteMsg")
    void batchRouteMsg(@RequestBody List<ImMsgBody> imMsgBodyList);

}
