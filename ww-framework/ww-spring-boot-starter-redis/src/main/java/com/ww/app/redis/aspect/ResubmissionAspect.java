package com.ww.app.redis.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.CaffeineUtil;
import com.ww.app.common.utils.SpringExpressionUtils;
import com.ww.app.redis.annotation.Resubmission;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author ww
 * @create 2023-09-05- 10:40
 * @description: 高性能、高可用的接口幂等性保障实现
 */
@Slf4j
@Aspect
@Component
public class ResubmissionAspect {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * Redis防重复提交键前缀
     */
    private static final String RESUBMISSION_PREFIX = "resubmission";

    /**
     * 本地缓存，使用Caffeine提供高性能
     */
    private Cache<String, Boolean> localCache;

    /**
     * 是否启用本地缓存
     */
    @Value("${resubmission.local-cache.enabled:true}")
    private boolean localCacheEnabled;

    /**
     * 本地缓存初始容量
     */
    @Value("${resubmission.local-cache.initial-capacity:100}")
    private int localCacheInitialCapacity;

    /**
     * 本地缓存最大容量
     */
    @Value("${resubmission.local-cache.maximum-size:10000}")
    private int localCacheMaximumSize;

    /**
     * 本地缓存过期时间（秒）
     */
    @Value("${resubmission.local-cache.expire-after-write:300}")
    private int localCacheExpireAfterWrite;

    /**
     * Redis异常时的降级策略
     * true: 允许请求继续
     * false: 拒绝请求
     */
    @Value("${resubmission.fallback.allow-when-redis-fail:false}")
    private boolean allowWhenRedisFail;

    /**
     * 初始化本地缓存
     */
    @PostConstruct
    public void init() {
        if (localCacheEnabled) {
            localCache = CaffeineUtil.createCache(localCacheInitialCapacity, localCacheMaximumSize, localCacheExpireAfterWrite, TimeUnit.SECONDS);
            log.info("初始化幂等性本地缓存完成，容量: {}, 过期时间: {}秒", localCacheMaximumSize, localCacheExpireAfterWrite);
        } else {
            log.info("本地缓存已禁用，将直接使用Redis进行幂等性检查");
        }
    }

    /**
     * 环绕通知，为标注了@Resubmission注解的方法提供防重复提交功能
     */
    @Around("@annotation(com.ww.app.redis.annotation.Resubmission)")
    public Object handleResubmission(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        // 1. 获取方法签名和注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Resubmission resubmission = method.getAnnotation(Resubmission.class);
        // 2. 构建唯一标识键
        String key = buildUniqueKey(joinPoint, signature, resubmission);
        try {
            // 3. 检查幂等性【使用Redis SET NX 和本地缓存进行常规检查】
            checkIdempotency(key, resubmission);
            // 4. 执行目标方法
            Object result = joinPoint.proceed();
            // 5. 记录执行耗时
            if (log.isDebugEnabled()) {
                long endTime = System.currentTimeMillis();
                log.debug("幂等性检查完成: key={}, 耗时={}ms", key, (endTime - startTime));
            }
            return result;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("执行防重复提交切面发生异常: key={}", key, e);
            throw e;
        }
    }

    /**
     * 构建请求的唯一标识键
     */
    private String buildUniqueKey(ProceedingJoinPoint joinPoint, MethodSignature signature, Resubmission resubmission) {
        StringBuilder keyBuilder = new StringBuilder(64);
        keyBuilder.append(RESUBMISSION_PREFIX).append(Constant.SPLIT);
        // 使用自定义键表达式
        if (StrUtil.isNotEmpty(resubmission.key())) {
            Object customKey = SpringExpressionUtils.parseExpression(joinPoint, resubmission.key());
            if (customKey != null) {
                keyBuilder.append(customKey);
                return keyBuilder.toString();
            }
        }
        // 默认使用方法签名+参数构建键
        String methodInfo = signature.getDeclaringTypeName() + StrUtil.DOT + signature.getName();
        String argsInfo = buildArgsInfo(joinPoint, resubmission);
        // 使用MD5避免键过长
        String uniqueId = SecureUtil.md5(methodInfo + Constant.SPLIT + argsInfo);
        keyBuilder.append(uniqueId);
        return keyBuilder.toString();
    }

    /**
     * 构建参数信息
     */
    private String buildArgsInfo(ProceedingJoinPoint joinPoint, Resubmission resubmission) {
        Object[] args = joinPoint.getArgs();
        // 如果指定了参数索引，则只使用这些索引的参数
        if (resubmission.paramIndexes().length > 0) {
            return Arrays.stream(resubmission.paramIndexes())
                    .filter(i -> i >= 0 && i < args.length)
                    .mapToObj(i -> args[i])
                    .map(this::safeToString)
                    .reduce("", (a, b) -> a + Constant.SPLIT + b);
        }
        // 默认使用所有参数
        return Arrays.stream(args)
                .map(this::safeToString)
                .reduce("", (a, b) -> a + Constant.SPLIT + b);
    }

    /**
     * 安全地将对象转换为字符串
     */
    private String safeToString(Object obj) {
        if (obj == null) {
            return StrUtil.NULL;
        }
        try {
            return obj.toString();
        } catch (Exception e) {
            log.warn("对象转换字符串异常", e);
            return obj.getClass().getName() + StrUtil.AT + Integer.toHexString(obj.hashCode());
        }
    }

    /**
     * 检查请求幂等性 (Redis SET NX + 本地缓存方式)
     */
    private void checkIdempotency(String key, Resubmission resubmission) {
        // 1. 首先检查本地缓存（如果启用）
        if (localCacheEnabled && Boolean.TRUE.equals(localCache.getIfPresent(key))) {
            if (log.isDebugEnabled()) {
                log.debug("本地缓存检测到重复请求: {}", key);
            }
            throw new ApiException(GlobalResCodeConstants.REPEATED_REQUESTS);
        }
        // 2. 检查Redis
        Boolean success = executeWithFallback(() -> {
            try {
                return stringRedisTemplate.execute(
                        (RedisCallback<Boolean>) connection -> connection.set(
                                key.getBytes(),
                                UUID.randomUUID().toString().getBytes(),
                                Expiration.from(resubmission.expire(), resubmission.timeUnit()),
                                RedisStringCommands.SetOption.SET_IF_ABSENT)
                );
            } catch (Exception e) {
                log.error("Redis SET NX操作异常: key={}", key, e);
                throw e;
            }
        }, resubmission.strictMode() || !allowWhenRedisFail);
        // 更新本地缓存
        if (localCacheEnabled) {
            localCache.put(key, true);
        }
        // 3. 如果Redis设置失败，表示存在重复请求
        if (!Boolean.TRUE.equals(success)) {
            if (log.isDebugEnabled()) {
                log.debug("Redis检测到重复请求: {}", key);
            }
            throw new ApiException(GlobalResCodeConstants.REPEATED_REQUESTS);
        }
    }

    /**
     * 执行Redis操作并提供降级策略
     */
    private <T> T executeWithFallback(Supplier<T> operation, boolean failFast) {
        try {
            return operation.get();
        } catch (Exception e) {
            log.error("幂等性检查操作失败，执行降级策略", e);
            if (failFast) {
                // 降级策略：拒绝请求
                throw new ApiException(GlobalResCodeConstants.SYSTEM_ERROR);
            } else {
                // 降级策略：允许执行业务
                return (T) Boolean.TRUE;
            }
        }
    }
}
