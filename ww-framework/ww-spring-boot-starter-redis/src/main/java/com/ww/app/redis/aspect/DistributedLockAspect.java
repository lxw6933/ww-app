package com.ww.app.redis.aspect;

import cn.hutool.crypto.SecureUtil;
import com.ww.app.common.constant.Constant;
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

/**
 * @author ww
 * @create 2023-11-30- 13:54
 * @description: 业务分布式锁
 */
@Slf4j
@Aspect
@Component
public class DistributedLockAspect {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private AppLockRedisKeyBuilder appLockRedisKeyBuilder;

    @Around("@annotation(com.ww.app.redis.annotation.DistributedLock)")
    public Object mallDistributedLockAdvise(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String classDeclaringTypeName = signature.getDeclaringTypeName();
        Method method = signature.getMethod();
        // 构建SpEL上下文，并设置变量值
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);
        // 生成key
        StringBuilder sb = new StringBuilder(128);

        sb.append(
                StringUtils.isNotEmpty(distributedLock.value()) ?
                SpringExpressionUtils.parseExpression(joinPoint, distributedLock.value()) : classDeclaringTypeName + Constant.SPLIT + method.getName()
        );
        if (StringUtils.isNotEmpty(distributedLock.userId())) {
            sb.append(Constant.SPLIT).append(SpringExpressionUtils.parseExpression(joinPoint, distributedLock.userId()));
        }
        if (StringUtils.isNotEmpty(distributedLock.operationKey())) {
            sb.append(Constant.SPLIT).append(SpringExpressionUtils.parseExpression(joinPoint, distributedLock.operationKey()));
        }
        String lockKey = appLockRedisKeyBuilder.buildLock(SecureUtil.md5(sb.toString()));
        RLock lock = redissonClient.getLock(lockKey);
        long currThreadId = Thread.currentThread().getId();
        try {
            log.info("线程[{}]尝试获取锁key：{}", currThreadId, lockKey);
            boolean successGetLock = lock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());
            if (!successGetLock) {
                throw new ApiException(GlobalResCodeConstants.LIMIT_REQUEST);
            }
            log.info("线程[{}]获取到锁key：{}", currThreadId, lockKey);
            return joinPoint.proceed();
        } catch (ApiException e) {
            throw new ApiException(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.info("线程[{}]锁key[{}]业务异常[{}]", currThreadId, lockKey, e.getMessage());
            throw new ApiException(GlobalResCodeConstants.SYSTEM_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                log.info("线程[{}]释放锁key：{}", currThreadId, lockKey);
                lock.unlock();
            }
        }
    }

}
