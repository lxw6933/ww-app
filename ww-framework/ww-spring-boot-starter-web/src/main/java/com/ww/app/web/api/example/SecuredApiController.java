package com.ww.app.web.api.example;

import com.ww.app.web.annotation.ApiSecured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 安全API示例控制器
 * 所有接口都需要添加以下请求头：
 * - X-App-Id: 应用标识
 * - X-Timestamp: 时间戳（秒级Unix时间戳）
 * - X-Sign: 签名值
 */
@RestController
@RequestMapping("/api/secured")
public class SecuredApiController {

    /**
     * 需要签名验证的接口
     * 请求头要求：
     * - X-App-Id: 应用标识
     * - X-Timestamp: 时间戳（秒级Unix时间戳）
     * - X-Sign: 签名值
     */
    @GetMapping("/verify")
    @ApiSecured
    public Map<String, Object> securedApi() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "签名验证成功");
        result.put("data", "这是一个需要签名验证的接口");
        return result;
    }

    /**
     * 不需要时间戳验证的接口
     * 请求头要求：
     * - X-App-Id: 应用标识
     * - X-Sign: 签名值
     */
    @GetMapping("/no-timestamp")
    @ApiSecured(enableTimestamp = false)
    public Map<String, Object> noTimestampApi() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "签名验证成功");
        result.put("data", "这是一个不需要时间戳验证的接口");
        return result;
    }
    
}