package com.ww.app.im.router.api.rpc;

import com.ww.app.im.common.ImMsgBody;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author ww
 * @create 2024-12-26- 10:47
 * @description:
 */
@FeignClient(value = "ww-im-router")
public interface ImRouterApi {

    @PostMapping("/ww-im-router/im/inner/routeMsg")
    void routeMsg(@RequestBody ImMsgBody imMsgBody);

    @PostMapping("/ww-im-router/im/inner/batchRouteMsg")
    void batchRouteMsg(@RequestBody List<ImMsgBody> imMsgBodyList);

}
