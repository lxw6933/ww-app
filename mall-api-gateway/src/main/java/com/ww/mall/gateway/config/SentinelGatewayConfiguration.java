package com.ww.mall.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.fastjson.JSON;
import com.ww.mall.common.common.Result;
import com.ww.mall.common.enums.CodeEnum;
import com.ww.mall.gateway.enums.GatewayResultEnum;
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
            Result<Object> result = new Result<>(GatewayResultEnum.TOO_MANY_REQUESTS.getCode(), GatewayResultEnum.TOO_MANY_REQUESTS.getMsg());
            return ServerResponse.ok().bodyValue(JSON.toJSONString(result));
        });
    }

}
