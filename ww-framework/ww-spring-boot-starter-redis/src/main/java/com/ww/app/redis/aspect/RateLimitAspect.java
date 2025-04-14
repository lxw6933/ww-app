package com.ww.app.redis.aspect;

import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.IpUtil;
import com.ww.app.redis.annotation.RateLimit;
import com.ww.app.redis.component.key.RateLimitRedisKeyBuilder;
import com.ww.app.redis.component.lua.RedisScriptComponent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author ww
 * @create 2023-09-05- 10:40
 * @description: 分布式限流切面，基于Redis实现，支持滑动窗口算法
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    private static final String RATE_LIMIT_SCRIPT_NAME = "rate_limit_script";

    @Resource
    private DefaultRedisScript<Long> rateLimitScript;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private RateLimitRedisKeyBuilder rateLimitRedisKeyBuilder;

    @Resource
    private RedisScriptComponent redisScriptComponent;

    @PostConstruct
    public void init() {
        // 加载脚本到Redis服务器
        redisScriptComponent.preLoadLuaScript(RATE_LIMIT_SCRIPT_NAME, rateLimitScript.getScriptAsString());
    }

    @Around("@annotation(com.ww.app.redis.annotation.RateLimit)")
    public Object rateLimitAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        
        if (rateLimit == null) {
            return joinPoint.proceed();
        }
        // 构建限流key
        String key = buildRateLimitKey(joinPoint, rateLimit);
        // 检查白名单
        if (rateLimit.enableWhitelist() && isInWhitelist(key)) {
            return joinPoint.proceed();
        }
        // 检查黑名单
        if (rateLimit.enableBlacklist() && isInBlacklist(key)) {
            log.warn("请求被黑名单拦截: key={}", key);
            throw new ApiException("请求被拒绝");
        }
        // 执行限流检查
        boolean isLimited = checkRateLimit(key, rateLimit);
        if (isLimited) {
            log.warn("触发限流: key={}, limit={}/{}", key, rateLimit.count(), rateLimit.period());
            // 根据降级策略处理
            switch (rateLimit.fallbackStrategy()) {
                case CACHE:
                    return handleCacheFallback(joinPoint, rateLimit);
                case FALLBACK:
                    return handleFallbackMethod(joinPoint, rateLimit);
                default:
                    throw new ApiException(rateLimit.message());
            }
        }
        return joinPoint.proceed();
    }
    
    /**
     * 构建限流key
     */
    private String buildRateLimitKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        // 获取类名和方法名
        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();
        // 获取限流类型
        String type = StringUtils.isNotBlank(rateLimit.type()) ? rateLimit.type() : "default";
        // 根据限流粒度构建key
        switch (rateLimit.limitType()) {
            case IP:
                return rateLimitRedisKeyBuilder.buildIpRateLimitKey(type, className, methodName, getClientIp());
            case USER:
                return rateLimitRedisKeyBuilder.buildUserRateLimitKey(type, className, methodName, getUserId());
            case IP_USER:
                return rateLimitRedisKeyBuilder.buildIpUserRateLimitKey(type, className, methodName, getClientIp(), getUserId());
            default:
                return rateLimitRedisKeyBuilder.buildRateLimitKey(type, className, methodName);
        }
    }
    
    /**
     * 检查是否触发限流
     */
    private boolean checkRateLimit(String key, RateLimit rateLimit) {
        try {
            // 执行限流脚本
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            byte[] periodBytes = String.valueOf(rateLimit.period()).getBytes(StandardCharsets.UTF_8);
            byte[] countBytes = String.valueOf(rateLimit.count()).getBytes(StandardCharsets.UTF_8);
            byte[] timestampBytes = String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8);
            Long result = redisScriptComponent.executeLuaScript(RATE_LIMIT_SCRIPT_NAME, ReturnType.INTEGER, 1, keyBytes, periodBytes, countBytes, timestampBytes);
            return Objects.equals(result, 0L);
        } catch (Exception e) {
            log.error("限流检查异常: key={}", key, e);
            // 发生异常时默认不触发限流，保证系统可用性
            return false;
        }
    }
    
    /**
     * 检查是否在白名单中
     */
    private boolean isInWhitelist(String key) {
        try {
            String whitelistKey = rateLimitRedisKeyBuilder.buildWhitelistKey();
            String value = getClientIp();
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(whitelistKey, value));
        } catch (Exception e) {
            log.error("白名单检查异常: key={}", key, e);
            return false;
        }
    }
    
    /**
     * 检查是否在黑名单中
     */
    private boolean isInBlacklist(String key) {
        try {
            String blacklistKey = rateLimitRedisKeyBuilder.buildBlacklistKey();
            String value = getClientIp();
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(blacklistKey, value));
        } catch (Exception e) {
            log.error("黑名单检查异常: key={}", key, e);
            return false;
        }
    }
    
    /**
     * 处理缓存降级
     */
    private Object handleCacheFallback(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        // TODO: 实现缓存降级逻辑
        log.info("执行缓存降级: {}", joinPoint.getSignature().getName());
        return null;
    }
    
    /**
     * 处理降级方法
     */
    private Object handleFallbackMethod(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // TODO: 实现降级方法调用逻辑
        log.info("执行降级方法: {}", rateLimit.fallbackMethod());
        return null;
    }
    
    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        return IpUtil.getRealIp();
    }
    
    /**
     * 获取用户ID
     */
    private String getUserId() {
        return AuthorizationContext.getClientUser().getId().toString();
    }
}
