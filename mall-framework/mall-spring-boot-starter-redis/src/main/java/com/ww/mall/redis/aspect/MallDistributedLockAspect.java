package com.ww.mall.redis.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.common.enums.GlobalResCodeConstants;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.common.utils.SpringExpressionUtils;
import com.ww.mall.redis.annotation.MallDistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @author ww
 * @create 2023-11-30- 13:54
 * @description: 业务分布式锁
 */
@Slf4j
@Aspect
@Component
public class MallDistributedLockAspect {

    @Autowired
    private RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "mall:lock";

    @Around("@annotation(com.ww.mall.redis.annotation.MallDistributedLock)")
    public Object mallDistributedLockAdvise(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String classDeclaringTypeName = signature.getDeclaringTypeName();
        Method method = signature.getMethod();
        // 构建SpEL上下文，并设置变量值
        MallDistributedLock mallDistributedLock = method.getAnnotation(MallDistributedLock.class);
        // 生成key
        StringBuilder sb = new StringBuilder(128);

        sb.append(
                StringUtils.isNotEmpty(mallDistributedLock.value()) ?
                SpringExpressionUtils.parseExpression(joinPoint, mallDistributedLock.value()) : classDeclaringTypeName + Constant.SPLIT + method.getName()
        );
        if (StringUtils.isNotEmpty(mallDistributedLock.userId())) {
            sb.append(Constant.SPLIT).append(SpringExpressionUtils.parseExpression(joinPoint, mallDistributedLock.userId()));
        }
        if (StringUtils.isNotEmpty(mallDistributedLock.operationKey())) {
            sb.append(Constant.SPLIT).append(SpringExpressionUtils.parseExpression(joinPoint, mallDistributedLock.operationKey()));
        }
        String lockKey = StrUtil.join(Constant.SPLIT, LOCK_PREFIX, SecureUtil.md5(sb.toString()));
        RLock lock = redissonClient.getLock(lockKey);
        try {
            log.info("线程[{}]尝试获取锁key：{}", Thread.currentThread().getId(), lockKey);
            boolean successGetLock = lock.tryLock(mallDistributedLock.waitTime(), mallDistributedLock.leaseTime(), mallDistributedLock.timeUnit());
            if (!successGetLock) {
                throw new ApiException(GlobalResCodeConstants.LIMIT_REQUEST);
            }
            log.info("线程[{}]获取到锁key：{}", Thread.currentThread().getId(), lockKey);
            return joinPoint.proceed();
        } catch (ApiException e) {
            throw new ApiException(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.info("线程[{}]锁key[{}]业务异常[{}]", Thread.currentThread().getId(), lockKey, e.getMessage());
            throw new ApiException(GlobalResCodeConstants.SYSTEM_ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                log.info("线程[{}]释放锁key：{}", Thread.currentThread().getId(), lockKey);
                lock.unlock();
            }
        }
    }

}
