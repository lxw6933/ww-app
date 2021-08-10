package com.ww.mall.utils;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @description: ip工具类
 * @author: ww
 * @create: 2021-05-12 19:57
 */
public class IpUtils {

    private static final String UNKNOWN = "unknown";

    private static final String LOCAL_HOST_IP = "0:0:0:0:0:0:0:1";

    private static final String LOCAL_HOST_IP_DEFAULT = "127.0.0.1";

    private IpUtils() {
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

}

