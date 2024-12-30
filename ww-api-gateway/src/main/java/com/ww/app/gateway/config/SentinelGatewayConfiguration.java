package com.ww.app.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.fastjson.JSON;
import com.ww.app.common.common.Result;
import com.ww.app.common.enums.GlobalResCodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/16 09:45
 **/
@Slf4j
@Configuration
public class SentinelGatewayConfiguration {

    public SentinelGatewayConfiguration() {
        // 网关sentinel限流回调
        GatewayCallbackManager.setBlockHandler((serverWebExchange, throwable) -> {
            log.error("网关限流回调: {}", serverWebExchange, throwable);
            Result<Object> result = Result.error(GlobalResCodeConstants.TOO_MANY_REQUESTS);
            return ServerResponse.ok().bodyValue(JSON.toJSONString(result));
        });
    }

}
