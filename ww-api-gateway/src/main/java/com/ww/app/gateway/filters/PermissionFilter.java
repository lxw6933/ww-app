package com.ww.app.gateway.filters;

import com.ww.app.common.constant.Constant;
import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.app.gateway.auth.AuthFacade;
import com.ww.app.gateway.auth.AuthResult;
import com.ww.app.gateway.utils.WebFluxResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2023-07-18- 13:40
 * @description:
 */
@Slf4j
@Order(2)
@Component
public class PermissionFilter implements GlobalFilter, Ordered {

    @Resource
    private AuthFacade authFacade;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String tokenInfo;
        String userType;

        try {
            AuthResult authResult = authFacade.authorize(request);
            if (!authResult.isAllowed()) {
                return WebFluxResultUtils.result(exchange, authResult.getErrorCode(), authResult.getHttpStatus());
            }
            // Carry authenticated context downstream as headers.
            tokenInfo = authResult.getTokenInfo();
            userType = authResult.getUserType();
        } catch (Exception e) {
            return WebFluxResultUtils.result(exchange, GlobalResCodeConstants.TOKEN_ERROR, HttpStatus.UNAUTHORIZED);
        }
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                .header(Constant.GATEWAY_REQUEST_FLAG, Constant.GATEWAY_REQUEST_FLAG_VALUE);
        if (StringUtils.isNotEmpty(tokenInfo)) {
            ServerHttpRequest permissionRequest = requestBuilder
                    .header(Constant.USER_TOKEN_INFO, tokenInfo)
                    .header(Constant.USER_TYPE, userType)
                    .build();
            return chain.filter(exchange.mutate().request(permissionRequest).build());
        } else {
            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
        }
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
