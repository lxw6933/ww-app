package com.ww.app.web.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * API安全客户端工具类
 * 用于演示客户端如何生成签名
 */
public class ApiSecurityClientUtils {

    /**
     * 生成签名
     *
     * @param params    请求参数
     * @return 签名
     */
    public static String generateSign(Map<String, String> params) {
        return SignUtils.generateSign(params);
    }

    /**
     * 获取时间戳（秒）
     */
    public static String getTimestamp() {
        return String.valueOf(System.currentTimeMillis() / 1000);
    }
    
    /**
     * 生成安全请求头
     * 
     * @param appId      应用ID
     * @param secretKey  密钥
     * @param params     请求参数
     * @return 安全请求头Map
     */
    public static Map<String, String> generateSecurityHeaders(String appId, String secretKey, Map<String, String> params) {
        Map<String, String> headers = new HashMap<>();
        
        // 添加应用ID请求头
        headers.put("X-App-Id", appId);
        
        // 添加时间戳请求头
        String timestamp = getTimestamp();
        headers.put("X-Timestamp", timestamp);
        
        // 构造签名参数
        Map<String, String> signParams = new HashMap<>(params);
        signParams.put("appId", appId);
        signParams.put("secret", secretKey);
        signParams.put("timestamp", timestamp);
        
        // 生成签名并添加到请求头
        String sign = generateSign(signParams);
        headers.put("X-Sign", sign);
        
        return headers;
    }

    /**
     * 客户端请求示例
     */
    public static void main(String[] args) {
        // 客户端参数
        String appId = "test_app";
        String secretKey = "test_secret_key";
        
        // 构造请求参数
        Map<String, String> params = new HashMap<>();
        params.put("name", "测试名称");
        params.put("age", "25");
        
        // 生成安全请求头
        Map<String, String> securityHeaders = generateSecurityHeaders(appId, secretKey, params);
        
        System.out.println("安全请求头: " + securityHeaders);
        
        // 实际调用API时，需将安全请求头一起发送
        // 以下是伪代码示例
        /*
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.example.com/data"))
            .header("X-App-Id", securityHeaders.get("X-App-Id"))
            .header("X-Timestamp", securityHeaders.get("X-Timestamp"))
            .header("X-Sign", securityHeaders.get("X-Sign"))
            .GET()
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        */
    }
} 