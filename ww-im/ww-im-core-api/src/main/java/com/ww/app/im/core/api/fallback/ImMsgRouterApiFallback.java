package com.ww.app.im.core.api.fallback;

import com.ww.app.common.common.Result;
import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.app.im.core.api.common.ImMsgBody;
import com.ww.app.im.core.api.rpc.ImMsgRouterApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.List;

/**
 * @author ww
 * @create 2024-12-24 21:31
 * @description:
 */
@Slf4j
public class ImMsgRouterApiFallback implements FallbackFactory<ImMsgRouterApi> {

    @Override
    public ImMsgRouterApi create(Throwable cause) {
        log.error("im server异常", cause);
        return new ImMsgRouterApi() {
            @Override
            public Result<Boolean> sendMsg(ImMsgBody imMsgBody) {
                return Result.error(GlobalResCodeConstants.LIMIT_REQUEST);
            }

            @Override
            public Result<Boolean> batchSendMsg(List<ImMsgBody> imMsgBodyList) {
                return Result.error(GlobalResCodeConstants.LIMIT_REQUEST);
            }
        };
    }
}
