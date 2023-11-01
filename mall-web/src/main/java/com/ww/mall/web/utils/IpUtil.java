package com.ww.mall.web.utils;

import com.alibaba.fastjson.JSONObject;
import com.ww.mall.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.core.io.ClassPathResource;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author ww
 * @create 2023-07-15- 14:49
 * @description:
 */
@Slf4j
public class IpUtil {

    private static final String UNKNOWN = "unknown";

    private static final String LOCAL_HOST_IP = "0:0:0:0:0:0:0:1";

    private static final String LOCAL_HOST_IP_DEFAULT = "127.0.0.1";

    private IpUtil() {
    }

    public static String getIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            // HTTP_CLIENT_IP：有些代理服务器
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            // X-Real-IP：nginx服务代理
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (LOCAL_HOST_IP.equals(ip)) {
            // 根据网卡取本机配置的IP
            InetAddress inetAddress = null;
            try {
                inetAddress = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                log.error("本地ip解析异常UnknownHostException", e);
            }
            ip = inetAddress == null ? LOCAL_HOST_IP_DEFAULT : inetAddress.getHostAddress();
        }
        return ip;
    }

    public static String getRealIp(HttpServletRequest request) {
        String ipStr = getIp(request);
        log.info("请求ipStr：{}", ipStr);
        String[] ipArr = ipStr.split(",");
        String realIp = ipArr[ipArr.length - 1].trim();
        log.info("真实请求ip：{}", realIp);
        return realIp;
    }

    public static void validIp(List<String> whiteIpList, HttpServletRequest request) {
        // 校验ip
        if (CollectionUtils.isEmpty(whiteIpList)) {
            log.error("ip白名单未配置");
            throw new ApiException("ip白名单校验失败");
        }
        String ip = getRealIp(request);
        if (!whiteIpList.contains(ip)) {
            log.error("ip白名单校验失败");
            throw new ApiException("非法ip请求");
        }
    }

}
