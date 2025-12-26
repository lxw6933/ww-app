package com.ww.app.open.common;

import com.ww.app.open.entity.OpenApiInfo;
import com.ww.app.open.entity.OpenApplication;
import lombok.Data;

/**
 * 开放平台API上下文
 * 
 * @author ww
 * @create 2024-05-27
 * @description: 存储当前请求的上下文信息，用于在拦截器和业务代码中共享
 * 使用ThreadLocal实现线程隔离，避免使用request.setAttribute依赖HttpServletRequest
 */
@Data
public class OpenApiContext {

    /**
     * 流水号
     */
    private String transId;

    /**
     * 商户编码
     */
    private String sysCode;

    /**
     * 应用编码
     */
    private String appCode;

    /**
     * API编码
     */
    private String apiCode;

    /**
     * 请求IP
     */
    private String requestIp;

    /**
     * 请求开始时间
     */
    private Long requestStartTime;

    /**
     * 用户ID（如果API需要登录）
     */
    private Long userId;

    /**
     * 请求对象（包含请求参数和签名等信息）
     */
    private BaseOpenRequest<?> openRequest;

    /**
     * 应用信息
     */
    private OpenApplication openApplication;

    /**
     * API信息
     */
    private OpenApiInfo openApiInfo;

    private static final ThreadLocal<OpenApiContext> CONTEXT = new ThreadLocal<>();

    public static void set(OpenApiContext context) {
        CONTEXT.set(context);
    }

    public static OpenApiContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}


