package com.ww.mall.gateway.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

/**
 * @author ww
 * @create 2023-08-01- 15:59
 * @description: 网关ip工具类
 */
@Slf4j
public class GatewayIpUtil {

    private GatewayIpUtil() {}

    private static final String IP_UNKNOWN = "unknown";
    private static final String IPV4_LOCAL_IP = "127.0.0.1";
    private static final String IPV6_LOCAL_IP = "0:0:0:0:0:0:0:1";

    public static String getIpAddress(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        String ipAddress = headers.getFirst("x-forwarded-for");
        if (ipAddress == null || ipAddress.isEmpty() || IP_UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = headers.getFirst("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || IP_UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = headers.getFirst("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || IP_UNKNOWN.equalsIgnoreCase(ipAddress)) {
            // HTTP_CLIENT_IP：有些代理服务器
            ipAddress = headers.getFirst("HTTP_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || IP_UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = headers.getFirst("X-Real-Ip");
        }
        if (ipAddress == null || ipAddress.isEmpty() || IP_UNKNOWN.equalsIgnoreCase(ipAddress)) {
            ipAddress = Optional.ofNullable(request.getRemoteAddress())
                    .map(address -> address.getAddress().getHostAddress())
                    .orElse("");
            if (IPV4_LOCAL_IP.equals(ipAddress) || IPV6_LOCAL_IP.equals(ipAddress)) {
                // 根据网卡取本机配置的IP
                try {
                    InetAddress inet = InetAddress.getLocalHost();
                    ipAddress = inet.getHostAddress();
                } catch (UnknownHostException e) {
                    log.error("网关获取ip异常：{}", e.getMessage());
                }
            }
        }
        // 对于通过多个代理的情况，第一个IP为客户端真实IP，多个IP按照','分割
        if (StringUtils.isNotEmpty(ipAddress)) {
            ipAddress = ipAddress.split(",")[0];
        }
        return ipAddress;
    }
}

