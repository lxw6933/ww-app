package com.ww.mall.gateway.filters;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @description:
 * @author: ww
 * @create: 2021/7/4 下午2:00
 **/
@Slf4j
@Component
public class MyGlobalGatewayFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID = "TRACE_ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = IdUtil.objectId();
        // 1.将traceId传递给微服务
        ServerHttpRequest request = exchange.getRequest().mutate().header("traceId", traceId).build();
        // 2.将traceId设置到slf4j中，日志打印模板配置打印traceId
        MDC.put(TRACE_ID, traceId);
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
