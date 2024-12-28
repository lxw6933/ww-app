package com.ww.mall.im.fallback;

import com.ww.mall.common.common.Result;
import com.ww.mall.common.enums.GlobalResCodeConstants;
import com.ww.mall.im.common.ImMsgBody;
import com.ww.mall.im.rpc.ImMsgRouterApi;
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
