package com.ww.mall.im.rpc;

import com.ww.mall.im.common.ImMsgBody;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author ww
 * @create 2024-12-26- 10:36
 * @description: 测试流程用
 */
@FeignClient(value = "mall-im-biz")
public interface BizMsgHandlerApi {

    @PostMapping("/mall-im-biz/im/inner/handleImMsg")
    void handleImMsg(@RequestBody ImMsgBody imMsgBody);

}
