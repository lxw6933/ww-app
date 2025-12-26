package com.ww.app.open.applications;

import com.ww.app.open.common.OpenApiContext;
import com.ww.app.open.common.OpenApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 开放平台API控制器示例
 * 
 * @author ww
 * @create 2024-05-27
 * @description: 提供开放平台API接口示例，展示如何使用开放平台功能
 */
@Slf4j
@RestController
@RequestMapping("/open/api")
public class OpenApiController {

    /**
     * 示例API：获取用户信息
     * 
     * @return 用户信息
     */
    @PostMapping("/user/info")
    public OpenApiResponse<Map<String, Object>> getUserInfo(@RequestBody Map<String, Object> params) {
        OpenApiContext context = OpenApiContext.get();
        
        log.info("处理API请求: appCode={}, apiCode={}, transId={}", 
                 context.getAppCode(), context.getApiCode(), context.getTransId());
        
        // 业务逻辑处理
        Map<String, Object> result = new HashMap<>();
        result.put("userId", params.get("userId"));
        result.put("userName", "示例用户");
        result.put("email", "example@example.com");
        
        return OpenApiResponse.success(result, context.getTransId(), context.getAppCode());
    }

    /**
     * 示例API：创建订单
     * 
     * @param params 订单参数
     * @return 订单信息
     */
    @PostMapping("/order/create")
    public OpenApiResponse<Map<String, Object>> createOrder(@RequestBody Map<String, Object> params) {
        OpenApiContext context = OpenApiContext.get();
        
        log.info("创建订单: appCode={}, transId={}", context.getAppCode(), context.getTransId());
        
        // 业务逻辑处理
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", System.currentTimeMillis());
        result.put("orderNo", "ORD" + System.currentTimeMillis());
        result.put("status", "SUCCESS");
        
        return OpenApiResponse.success(result, context.getTransId(), context.getAppCode());
    }

    /**
     * 健康检查接口（不需要签名验证）
     * 
     * @return 健康状态
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}


