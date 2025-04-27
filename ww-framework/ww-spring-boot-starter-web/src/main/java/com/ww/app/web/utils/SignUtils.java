package com.ww.app.web.utils;

import com.ww.app.common.utils.json.JacksonUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 签名工具类
 */
public class SignUtils {

    /**
     * 生成签名
     *
     * @param params    请求参数
     * @return 签名
     */
    public static String generateSign(Map<String, String> params) {
        // 1. 参数排序
        TreeMap<String, String> sortedParams = new TreeMap<>(params);
        // 2. 拼接参数
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (StringUtils.isNotBlank(value)) {
                sb.append(key).append("=").append(value).append("&");
            }
        }
        // 3. 生成MD5签名
        return DigestUtils.md5DigestAsHex(sb.toString().getBytes(StandardCharsets.UTF_8)).toUpperCase();
    }

    /**
     * 从请求头获取指定参数
     *
     * @param request    HTTP请求
     * @param headerName 请求头名称
     * @return 参数值
     */
    public static String getHeaderValue(HttpServletRequest request, String headerName) {
        return request.getHeader(headerName);
    }

    /**
     * 从请求中获取所有参数(URL参数)
     *
     * @param request HTTP请求
     * @return 参数Map
     */
    public static Map<String, String> getRequestParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        // 获取请求参数
        Map<String, String[]> requestParams = request.getParameterMap();
        if (!CollectionUtils.isEmpty(requestParams)) {
            for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
                String key = entry.getKey();
                String[] values = entry.getValue();
                if (values != null && values.length > 0) {
                    params.put(key, values[0]);
                }
            }
        }
        return params;
    }
    
    /**
     * 获取请求体中的JSON参数
     *
     * @param request HTTP请求
     * @return 参数Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> getRequestBodyParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        // 只处理JSON请求体
        if (isJsonRequest(request)) {
            try {
                // 读取请求体内容
                String body = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
                if (StringUtils.isNotBlank(body)) {
                    // 解析JSON数据
                    Map<String, Object> jsonMap = JacksonUtils.getObjectMapper().readValue(body, Map.class);
                    if (jsonMap != null) {
                        for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
                            String key = entry.getKey();
                            Object value = entry.getValue();
                            if (value != null) {
                                params.put(key, String.valueOf(value));
                            }
                        }
                    }
                }
            } catch (IOException e) {
                // 记录日志或处理异常
                throw new RuntimeException("解析请求体参数失败", e);
            }
        }
        return params;
    }
    
    /**
     * 判断是否为JSON请求
     *
     * @param request HTTP请求
     * @return 是否为JSON请求
     */
    public static boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * 验证时间戳是否有效
     *
     * @param timestamp      时间戳
     * @param expireSeconds  有效期(秒)
     * @return 是否有效
     */
    public static boolean isTimestampValid(String timestamp, long expireSeconds) {
        if (StringUtils.isBlank(timestamp)) {
            return false;
        }
        try {
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis() / 1000;
            return Math.abs(currentTime - requestTime) <= expireSeconds;
        } catch (NumberFormatException e) {
            return false;
        }
    }
} 