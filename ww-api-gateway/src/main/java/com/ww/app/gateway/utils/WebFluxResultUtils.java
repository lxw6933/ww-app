package com.ww.app.gateway.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.common.http.param.MediaType;
import com.ww.app.common.common.ResCode;
import com.ww.app.common.common.Result;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * @author ww
 * @create 2023-07-18- 14:50
 * @description:
 */
public final class WebFluxResultUtils {

    public WebFluxResultUtils() {}

    public static Mono<Void> result(final ServerWebExchange exchange, final ResCode resCode, final HttpStatus status) {
        Result<Object> result = Result.error(resCode);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap((Objects.requireNonNull(JSON.toJSONString(result))).getBytes())));
    }

}
