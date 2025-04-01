package com.ww.app.common.utils;

import cn.hutool.core.net.NetUtil;
import com.ww.app.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * IP工具类
 * 提供IP地址获取、验证、转换等功能
 *
 * @author ww
 * @create 2023-07-15- 14:49
 */
@Slf4j
public class IpUtil {

    private static final String UNKNOWN = "unknown";
    private static final String LOCAL_HOST_IP = "0:0:0:0:0:0:0:1";
    private static final String LOCAL_HOST_IP_DEFAULT = "127.0.0.1";
    private static final String[] IP_HEADERS = {
            "x-forwarded-for",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "X-Real-IP",
            "X-Forwarded-For"
    };
    
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");
    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");

    private IpUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 获取请求的真实IP地址
     * 按照优先级依次尝试从请求头中获取IP地址
     *
     * @param request HTTP请求
     * @return IP地址
     */
    public static String getIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        String ip = null;
        for (String header : IP_HEADERS) {
            ip = request.getHeader(header);
            if (isValidIp(ip)) {
                break;
            }
        }

        if (!isValidIp(ip)) {
            ip = request.getRemoteAddr();
        }

        if (LOCAL_HOST_IP.equals(ip)) {
            ip = getLocalIp();
        }

        return ip;
    }

    /**
     * 获取请求的真实IP地址（处理代理情况）
     *
     * @param request HTTP请求
     * @return 真实IP地址
     */
    public static String getRealIp(HttpServletRequest request) {
        String ipStr = getIp(request);
        if (StringUtils.isBlank(ipStr)) {
            return UNKNOWN;
        }

        // 处理多个IP的情况，取第一个非内网IP
        String[] ipArr = ipStr.split(",");
        for (String ip : ipArr) {
            ip = ip.trim();
            if (isValidIp(ip) && !isInternalIp(ip)) {
                return ip;
            }
        }

        return ipArr[ipArr.length - 1].trim();
    }

    /**
     * 验证IP是否在白名单中
     *
     * @param whiteIpList IP白名单列表
     * @param request HTTP请求
     */
    public static void validIp(List<String> whiteIpList, HttpServletRequest request) {
        validIp(whiteIpList, request, false);
    }

    /**
     * 验证IP是否在白名单中（支持IP段）
     *
     * @param whiteIpList IP白名单列表
     * @param request HTTP请求
     * @param ipRange 是否支持IP段
     */
    public static void validIp(List<String> whiteIpList, HttpServletRequest request, boolean ipRange) {
        String ip = getRealIp(request);
        validIpStr(whiteIpList, ip, ipRange);
    }

    /**
     * 验证IP字符串是否在白名单中
     *
     * @param whiteIpList IP白名单列表
     * @param ip IP地址
     */
    public static void validIpStr(List<String> whiteIpList, String ip) {
        validIpStr(whiteIpList, ip, false);
    }

    /**
     * 验证IP字符串是否在白名单中（支持IP段）
     *
     * @param whiteIpList IP白名单列表
     * @param ip IP地址
     * @param ipRange 是否支持IP段
     */
    public static void validIpStr(List<String> whiteIpList, String ip, boolean ipRange) {
        if (CollectionUtils.isEmpty(whiteIpList)) {
            throw new ApiException("IP白名单配置为空");
        }

        if (!isValidIp(ip)) {
            throw new ApiException("无效的IP地址: " + ip);
        }

        boolean isAllowed;
        if (ipRange) {
            isAllowed = whiteIpList.stream().anyMatch(ipRangeWhite -> NetUtil.isInRange(ip, ipRangeWhite));
        } else {
            isAllowed = whiteIpList.contains(ip);
        }

        if (!isAllowed) {
            throw new ApiException("IP地址不在白名单中: " + ip);
        }
    }

    /**
     * 获取本机IP地址
     *
     * @return 本机IP地址
     */
    public static String getLocalIp() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            return localHost.getHostAddress();
        } catch (UnknownHostException e) {
            log.error("获取本地IP地址失败", e);
            return LOCAL_HOST_IP_DEFAULT;
        }
    }

    /**
     * 获取所有本机IP地址
     *
     * @return IP地址列表
     */
    public static List<String> getAllLocalIps() {
        return new ArrayList<>(NetUtil.localIps());
    }

    /**
     * 检查IP地址是否有效
     *
     * @param ip IP地址
     * @return 是否有效
     */
    public static boolean isValidIp(String ip) {
        if (StringUtils.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            return false;
        }
        return IPV4_PATTERN.matcher(ip).matches() || IPV6_PATTERN.matcher(ip).matches();
    }

    /**
     * 检查IP地址是否为内网IP
     *
     * @param ip IP地址
     * @return 是否为内网IP
     */
    public static boolean isInternalIp(String ip) {
        if (!isValidIp(ip)) {
            return false;
        }
        return NetUtil.isInnerIP(ip);
    }

    /**
     * 将IP地址转换为长整型
     *
     * @param ip IP地址
     * @return 长整型值
     */
    public static long ipToLong(String ip) {
        if (!isValidIp(ip)) {
            throw new IllegalArgumentException("Invalid IP address: " + ip);
        }
        return NetUtil.ipv4ToLong(ip);
    }

    /**
     * 将长整型转换为IP地址
     *
     * @param ipLong 长整型值
     * @return IP地址
     */
    public static String longToIp(long ipLong) {
        return NetUtil.longToIpv4(ipLong);
    }

    /**
     * 检查IP地址是否在指定范围内
     *
     * @param ip IP地址
     * @param startIp 起始IP
     * @param endIp 结束IP
     * @return 是否在范围内
     */
    public static boolean isIpInRange(String ip, String startIp, String endIp) {
        if (!isValidIp(ip) || !isValidIp(startIp) || !isValidIp(endIp)) {
            return false;
        }
        long ipLong = ipToLong(ip);
        long startIpLong = ipToLong(startIp);
        long endIpLong = ipToLong(endIp);
        return ipLong >= startIpLong && ipLong <= endIpLong;
    }
}
