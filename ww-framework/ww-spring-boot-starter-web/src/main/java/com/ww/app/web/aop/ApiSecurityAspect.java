package com.ww.app.web.aop;

import com.ww.app.common.utils.HttpContextUtils;
import com.ww.app.web.annotation.ApiSecured;
import com.ww.app.web.constants.ApiSecurityConstant;
import com.ww.app.web.properties.ApiSecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * API安全切面
 * 使用AOP方式处理接口签名验证
 */
@Slf4j
@Aspect
@Component
public class ApiSecurityAspect {

    private final ApiSecurityProperties properties;

    public ApiSecurityAspect(ApiSecurityProperties properties) {
        this.properties = properties;
    }

    /**
     * 定义切入点：所有使用@ApiSecured注解的方法
     */
    @Pointcut("@annotation(com.ww.app.web.annotation.ApiSecured)")
    public void apiSecuredMethod() {
    }

    /**
     * 定义切入点：所有使用@ApiSecured注解的类中的所有方法
     */
    @Pointcut("within(@com.ww.app.web.annotation.ApiSecured *)")
    public void apiSecuredClass() {
    }

    @Around("apiSecuredMethod() || apiSecuredClass()")
    public Object validateApiSecurity(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取当前请求
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = attributes.getRequest();
        // 检查是否启用API安全
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        // 获取方法或类上的ApiSecured注解
        ApiSecured apiSecured = AnnotationUtils.findAnnotation(method, ApiSecured.class);
        if (apiSecured == null) {
            apiSecured = AnnotationUtils.findAnnotation(method.getDeclaringClass(), ApiSecured.class);
        }
        // 如果没有注解，则直接执行原方法
        if (apiSecured == null) {
            return joinPoint.proceed();
        }
        // 验证签名
        validateSignature(request, apiSecured);
        // 验证通过，执行原方法
        return joinPoint.proceed();
    }

    /**
     * 验证请求签名
     */
    private void validateSignature(HttpServletRequest request, ApiSecured apiSecured) {
        // 从请求头中获取客户端标识
        String appId = HttpContextUtils.getHeaderValue(request, ApiSecurityConstant.APP_ID);
        if (!StringUtils.hasText(appId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少应用标识请求头");
        }
        // 获取密钥
        String secretKey = properties.getSecrets().get(appId);
        if (!StringUtils.hasText(secretKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无效的应用标识");
        }
        // 验证时间戳
        String timestamp = HttpContextUtils.getHeaderValue(request, ApiSecurityConstant.APP_TIMESTAMP);
        if (apiSecured.enableTimestamp()) {
            if (!StringUtils.hasText(timestamp)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少时间戳请求头");
            }
            long expireSeconds = apiSecured.timestampExpire() > 0 ?
                    apiSecured.timestampExpire() : properties.getTimestampExpire();
            if (!isTimestampValid(timestamp, expireSeconds)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "时间戳已过期");
            }
        }
        // 验证签名
        String sign = HttpContextUtils.getHeaderValue(request, ApiSecurityConstant.APP_SIGN);
        if (!StringUtils.hasText(sign)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少api签名请求头");
        }
        // 获取所有URL参数
        Map<String, Object> params = HttpContextUtils.isJsonRequest(request) ?
                HttpContextUtils.getRequestBodyParams(request) : HttpContextUtils.getRequestParams(request);
        // 添加特定请求头参数
        params.put(ApiSecurityConstant.APP_ID, appId);
        params.put(ApiSecurityConstant.APP_SECRET, secretKey);
        if (apiSecured.enableTimestamp()) {
            params.put(ApiSecurityConstant.APP_TIMESTAMP, timestamp);
        }
        // 生成签名
        String singParams = generateSignParams(params);
        String serverSign = DigestUtils.md5DigestAsHex(singParams.getBytes(StandardCharsets.UTF_8)).toUpperCase();
        // 比较签名
        if (!sign.equals(serverSign)) {
            log.warn("签名验证失败，URL: {}, 客户端签名: {}, 服务端签名: {}, 签名参数: {}", request.getRequestURI(), sign, serverSign, singParams);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "签名验证失败");
        }
    }

    /**
     * 验证时间戳是否有效
     *
     * @param timestamp      时间戳
     * @param expireSeconds  有效期(秒)
     * @return 是否有效
     */
    public static boolean isTimestampValid(String timestamp, long expireSeconds) {
        if (org.apache.commons.lang3.StringUtils.isBlank(timestamp)) {
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

    /**
     * 生成签名
     *
     * @param params    请求参数
     * @return 签名
     */
    public static String generateSignParams(Map<String, Object> params) {
        // 1. 参数排序
        if (!(params instanceof TreeMap)) {
            params = new TreeMap<>(params);
        }
        // 2. 拼接参数
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();
            if (org.apache.commons.lang3.StringUtils.isNotBlank(value)) {
                sb.append(key).append("=").append(value).append("&");
            }
        }
        return sb.toString();
    }

} 