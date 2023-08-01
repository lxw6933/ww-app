package com.ww.mall.gateway.filters;

import cn.hutool.extra.servlet.ServletUtil;
import com.alibaba.fastjson.JSON;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.gateway.config.ServerGrayProperty;
import com.ww.mall.gateway.utils.GatewayIpUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author ww
 * @create 2023-08-01- 16:16
 * @description:
 */
@Slf4j
@Component
@AllArgsConstructor
public class IpFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String userRealIp = GatewayIpUtil.getIpAddress(exchange.getRequest());
        ServerHttpRequest permissionRequest = exchange.getRequest()
                .mutate()
                .header(Constant.USER_REAL_IP, userRealIp)
                .build();
        return chain.filter(exchange.mutate().request(permissionRequest).build());
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
