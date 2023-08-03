package com.ww.mall.gateway.filters;

import com.ww.mall.common.constant.Constant;
import com.ww.mall.gateway.config.ServerGrayProperty;
import com.ww.mall.gateway.utils.GatewayIpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author ww
 * @create 2023-08-01- 16:16
 * @description:
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpFilter implements GlobalFilter, Ordered {

    /**
     * 灰度自定义属性
     */
    private final ServerGrayProperty serverGrayProperty;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String userRealIp = GatewayIpUtil.getIpAddress(exchange.getRequest());
        // 是否开启灰度
        Boolean enableGray = serverGrayProperty.getEnable();
        // 获取配置的灰度版本
        String grayVersion = serverGrayProperty.getGrayVersion();
        String prodVersion = serverGrayProperty.getProVersion();
        // 获取配置的灰度ip白名单
        List<String> grayIps = serverGrayProperty.getGrayIps();
        boolean grayIpFlag = CollectionUtils.isNotEmpty(grayIps) && grayIps.contains(userRealIp);
        log.info("是否开启灰度:【{}】生产版本:【{}】灰度版本:【{}】请求ip是否为灰度:【{}】",
                enableGray, prodVersion, grayVersion, grayIpFlag);
        ServerHttpRequest ipRequest;
        if (Boolean.TRUE.equals(enableGray) && grayIpFlag) {
            ipRequest = exchange.getRequest()
                    .mutate()
                    .header(Constant.GRAY_VERSION, grayVersion)
                    .header(Constant.PROD_VERSION, prodVersion)
                    .header(Constant.GRAY_TAG, Constant.GRAY_TAG_VALUE)
                    .header(Constant.USER_REAL_IP, userRealIp)
                    .build();
        } else {
            ipRequest = exchange.getRequest()
                    .mutate()
                    .header(Constant.GRAY_VERSION, grayVersion)
                    .header(Constant.PROD_VERSION, prodVersion)
                    .header(Constant.USER_REAL_IP, userRealIp)
                    .build();
        }
        return chain.filter(exchange.mutate().request(ipRequest).build());
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
