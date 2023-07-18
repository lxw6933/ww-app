package com.ww.mall.gateway.utils;

import com.alibaba.fastjson.JSON;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * @author ww
 * @create 2023-07-18- 14:50
 * @description:
 */
public final class WebFluxResultUtils {

    public WebFluxResultUtils() {
    }

    public static Mono<Void> result(final ServerWebExchange exchange, final Object result) {
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap((Objects.requireNonNull(JSON.toJSONString(result))).getBytes())));
    }

}
