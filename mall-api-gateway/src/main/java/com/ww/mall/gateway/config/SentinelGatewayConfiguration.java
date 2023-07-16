package com.ww.mall.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.fastjson.JSON;
import com.ww.mall.common.common.Result;
import com.ww.mall.common.enums.CodeEnum;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/16 09:45
 **/
@Configuration
public class SentinelGatewayConfiguration {

    public SentinelGatewayConfiguration() {
        // 网关sentinel限流回调
        GatewayCallbackManager.setBlockHandler((serverWebExchange, throwable) -> {
            Result<Object> result = new Result<>(CodeEnum.LIMIT_ERROR.getCode(), CodeEnum.LIMIT_ERROR.getMessage());
            return ServerResponse.ok().bodyValue(JSON.toJSONString(result));
        });
    }

}
