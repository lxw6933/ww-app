package com.ww.mall.gateway.filters;

import cn.hutool.core.date.DateUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.common.http.param.MediaType;
import com.ww.mall.common.common.MallClientUser;
import com.ww.mall.common.common.Result;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.gateway.enums.GatewayResultEnum;
import com.ww.mall.gateway.utils.WebFluxResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Date;

/**
 * @author ww
 * @create 2023-07-18- 13:40
 * @description:
 */
@Slf4j
@Order(2)
@Component
@RefreshScope
public class PermissionFilter implements GlobalFilter {

    @Value("${jwt.secret}")
    public String jwtSecret = Constant.SECRET_KEY;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String token = request.getHeaders().getFirst(Constant.USER_TOKEN);
        MallClientUser tokenInfo = null;
        if (token != null) {
            try {
                // 校验token
                boolean verify = JWTUtil.verify(token, jwtSecret.getBytes());
                if (!verify) {
                    log.error("token校验失败");
                    Result<Object> result = new Result<>(GatewayResultEnum.SIGN_IS_NOT_PASS.getCode(), GatewayResultEnum.SIGN_IS_NOT_PASS.getMsg());
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                    return WebFluxResultUtils.result(exchange, result);
                }
                // 解析token
                JWT jwt = JWTUtil.parseToken(token);
                tokenInfo = jwt.getPayload().getClaimsJson().toBean(MallClientUser.class);
                if (DateUtil.date().after(new Date(tokenInfo.getExp()))) {
                    // 已过期
                    Result<Object> result = new Result<>(GatewayResultEnum.SING_TIME_IS_TIMEOUT.getCode(), GatewayResultEnum.SING_TIME_IS_TIMEOUT.getMsg());
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                    return WebFluxResultUtils.result(exchange, result);
                }
            } catch (Exception e) {
                log.error("token解码失败");
                Result<Object> result = new Result<>(GatewayResultEnum.SIGN_IS_NOT_PASS.getCode(), GatewayResultEnum.SIGN_IS_NOT_PASS.getMsg());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                return WebFluxResultUtils.result(exchange, result);
            }
        }
        if (tokenInfo != null) {
            ServerHttpRequest permissionRequest = exchange.getRequest()
                    .mutate()
                    .header(Constant.USER_TOKEN_INFO, JSON.toJSONString(tokenInfo))
                    .build();
            return chain.filter(exchange.mutate().request(permissionRequest).build());
        } else {
            return chain.filter(exchange);
        }
    }
}
