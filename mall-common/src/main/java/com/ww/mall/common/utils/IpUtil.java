package com.ww.mall.common.utils;

import cn.hutool.core.net.NetUtil;
import com.ww.mall.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

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
        validIp(whiteIpList, request, false);
    }

    public static void validIp(List<String> whiteIpList, HttpServletRequest request, boolean ipRange) {
        String ip = getRealIp(request);
        validIpStr(whiteIpList, ip, ipRange);
    }

    private static void validIpStr(List<String> whiteIpList, String reqRealIp) {
        validIpStr(whiteIpList, reqRealIp);
    }

    private static void validIpStr(List<String> whiteIpList, String ip, boolean ipRange) {
        // 校验ip
        if (CollectionUtils.isEmpty(whiteIpList)) {
            throw new ApiException("ip白名单校验失败");
        }
        boolean flag = false;
        if (ipRange) {
            for (String ipRangeWhite : whiteIpList) {
                if (NetUtil.isInRange(ip, ipRangeWhite)) {
                    flag = true;
                    break;
                }
            }
        } else {
            if (whiteIpList.contains(ip)) {
                flag = true;
            }
        }
        if (!flag) {
            throw new ApiException("非法ip请求");
        }
    }

}
