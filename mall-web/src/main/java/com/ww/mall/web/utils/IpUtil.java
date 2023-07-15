package com.ww.mall.web.utils;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

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
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (LOCAL_HOST_IP.equals(ip)) {
            // 根据网卡取本机配置的IP
            InetAddress inetAddress = null;
            try {
                inetAddress = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            ip = inetAddress == null ? LOCAL_HOST_IP_DEFAULT : inetAddress.getHostAddress();
        }
        return ip;
    }

    public static String getLastIp(HttpServletRequest request) {
        String ipStr = getIp(request);
        log.info("第三方请求ipStr：{}", ipStr);
        String[] ipArr = ipStr.split(",");
        String realIp = ipArr[ipArr.length - 1].trim();
        log.info("第三方请求ip：{}", realIp);
        return realIp;
    }

    public static String getFirstIp(HttpServletRequest request) {
        String ipStr = getIp(request);
        log.info("第三方请求ipStr：{}", ipStr);
        String[] ipArr = ipStr.split(",");
        String realIp = ipArr[0].trim();
        log.info("第三方请求ip：{}", realIp);
        return realIp;
    }

}
