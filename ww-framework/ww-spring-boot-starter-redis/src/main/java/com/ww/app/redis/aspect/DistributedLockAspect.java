package com.ww.app.redis.aspect;

import cn.hutool.crypto.SecureUtil;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.SpringExpressionUtils;
import com.ww.app.redis.annotation.DistributedLock;
import com.ww.app.redis.component.key.AppLockRedisKeyBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 分布式锁切面实现
 * 通过AOP方式为标注了@DistributedLock注解的方法提供分布式锁功能
 */
@Slf4j
@Aspect
@Component
public class DistributedLockAspect {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private AppLockRedisKeyBuilder appLockRedisKeyBuilder;

    /**
     * 环绕通知，为标注了@DistributedLock注解的方法提供分布式锁
     *
     * @param joinPoint 连接点
     * @return 目标方法执行结果
     * @throws Throwable 可能抛出的异常
     */
    @Around("@annotation(com.ww.app.redis.annotation.DistributedLock)")
    public Object distributedLockAdvise(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 前置准备：获取方法签名和注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock lockAnnotation = method.getAnnotation(DistributedLock.class);
        
        // 2. 构建锁的key
        String lockKey = buildLockKey(joinPoint, signature, lockAnnotation);
        
        // 3. 获取锁并执行目标方法
        return executeWithLock(joinPoint, lockAnnotation, lockKey);
    }
    
    /**
     * 构建分布式锁的key
     */
    private String buildLockKey(ProceedingJoinPoint joinPoint, MethodSignature signature, DistributedLock lockAnnotation) {
        // 基础key：优先使用注解value，其次使用类名+方法名
        String baseKey = StringUtils.isNotEmpty(lockAnnotation.value()) ?
                Objects.toString(SpringExpressionUtils.parseExpression(joinPoint, lockAnnotation.value()), "") : 
                signature.getDeclaringTypeName() + Constant.SPLIT + signature.getMethod().getName();
        
        StringBuilder keyBuilder = new StringBuilder(128).append(baseKey);
        
        // 添加用户标识（如果启用）
        if (lockAnnotation.enableUserLock()) {
            ClientUser clientUser = AuthorizationContext.getClientUser();
            if (clientUser != null && clientUser.getId() != null) {
                keyBuilder.append(Constant.SPLIT).append(clientUser.getId().toString());
            }
        }
        
        // 添加操作key（如果存在）
        if (StringUtils.isNotEmpty(lockAnnotation.operationKey())) {
            Object operationKeyObj = SpringExpressionUtils.parseExpression(joinPoint, lockAnnotation.operationKey());
            String operationKey = operationKeyObj != null ? operationKeyObj.toString() : "";
            if (StringUtils.isNotEmpty(operationKey)) {
                keyBuilder.append(Constant.SPLIT).append(operationKey);
            }
        }
        
        // 使用MD5处理，保证key长度合理且唯一
        return appLockRedisKeyBuilder.buildLock(SecureUtil.md5(keyBuilder.toString()));
    }
    
    /**
     * 使用分布式锁执行目标方法
     */
    private Object executeWithLock(ProceedingJoinPoint joinPoint, DistributedLock lockAnnotation, String lockKey) throws Throwable {
        RLock lock = redissonClient.getLock(lockKey);
        long threadId = Thread.currentThread().getId();
        long startTime = System.currentTimeMillis();
        boolean lockAcquired = false;
        
        try {
            // 尝试获取锁
            if (log.isDebugEnabled()) {
                log.debug("线程[{}]尝试获取锁: {}", threadId, lockKey);
            }
            
            lockAcquired = lock.tryLock(
                    lockAnnotation.waitTime(), 
                    lockAnnotation.leaseTime(), 
                    lockAnnotation.timeUnit()
            );
            
            if (!lockAcquired) {
                throw new ApiException(GlobalResCodeConstants.LIMIT_REQUEST);
            }
            
            if (log.isDebugEnabled()) {
                log.debug("线程[{}]获取到锁: {}", threadId, lockKey);
            }
            
            // 执行目标方法
            return joinPoint.proceed();
        } catch (ApiException e) {
            log.error("线程[{}]执行加锁方法出现业务异常, 锁: {}, 消息: {}", threadId, lockKey, e.getMessage());
            throw e; // 直接抛出业务异常，保留原始异常信息
        } catch (Exception e) {
            log.error("线程[{}]执行加锁方法出现系统异常, 锁: {}", threadId, lockKey, e);
            throw new ApiException(GlobalResCodeConstants.SYSTEM_ERROR);
        } finally {
            // 释放锁
            if (lockAcquired && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                    if (log.isDebugEnabled()) {
                        log.debug("线程[{}]释放锁: {}, 执行时间: {}ms", threadId, lockKey, (System.currentTimeMillis() - startTime));
                    }
                } catch (Exception e) {
                    log.error("线程[{}]释放锁异常, 锁: {}", threadId, lockKey, e);
                }
            }
        }
    }
}
