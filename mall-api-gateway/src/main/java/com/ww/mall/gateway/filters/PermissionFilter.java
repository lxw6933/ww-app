package com.ww.mall.gateway.filters;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.alibaba.fastjson.JSON;
import com.ww.mall.common.common.MallAdminUser;
import com.ww.mall.common.common.MallClientUser;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.common.enums.UserType;
import com.ww.mall.gateway.enums.GatewayResultEnum;
import com.ww.mall.gateway.utils.WebFluxResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
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
public class PermissionFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    public String jwtSecret = Constant.SECRET_KEY;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String token = request.getHeaders().getFirst(Constant.USER_TOKEN);
        String tokenInfo = null;
        if (token != null) {
            try {
                // 校验token
                boolean verify = JWTUtil.verify(token, jwtSecret.getBytes());
                if (!verify) {
                    log.error("token校验失败");
                    return WebFluxResultUtils.result(exchange, GatewayResultEnum.SIGN_IS_NOT_PASS, HttpStatus.UNAUTHORIZED);
                }
                // 解析token
                JWT jwt = JWTUtil.parseToken(token);
                JSONObject claimsJson = jwt.getPayload().getClaimsJson();
                if (DateUtil.date().after(new Date(claimsJson.get("exp", Long.class)))) {
                    // 已过期
                    return WebFluxResultUtils.result(exchange, GatewayResultEnum.SING_TIME_IS_TIMEOUT, HttpStatus.UNAUTHORIZED);
                }
                switch (claimsJson.get("userType", UserType.class)) {
                    case ADMIN:
                        MallAdminUser adminToken = claimsJson.toBean(MallAdminUser.class);
                        tokenInfo = JSON.toJSONString(adminToken);
                        break;
                    case CLIENT:
                        MallClientUser clientToken = claimsJson.toBean(MallClientUser.class);
                        tokenInfo = JSON.toJSONString(clientToken);
                        break;
                    case OTHER:
                        log.error("token用户类型为其他，系统暂不支持");
                        return WebFluxResultUtils.result(exchange, GatewayResultEnum.SIGN_IS_NOT_PASS, HttpStatus.UNAUTHORIZED);
                    default:
                        log.error("token用户类型异常");
                        return WebFluxResultUtils.result(exchange, GatewayResultEnum.SIGN_IS_NOT_PASS, HttpStatus.UNAUTHORIZED);
                }
            } catch (Exception e) {
                log.error("token解码失败");
                return WebFluxResultUtils.result(exchange, GatewayResultEnum.SIGN_IS_NOT_PASS, HttpStatus.UNAUTHORIZED);
            }
        }
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                .header(Constant.GATEWAY_REQUEST_FLAG, Constant.GATEWAY_REQUEST_FLAG_VALUE);
        if (StringUtils.isNotEmpty(tokenInfo)) {
            ServerHttpRequest permissionRequest = requestBuilder
                    .header(Constant.USER_TOKEN_INFO, tokenInfo)
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
