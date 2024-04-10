package com.ww.mall.redis.aspect;

import com.ww.mall.common.constant.Constant;
import com.ww.mall.common.enums.CodeEnum;
import com.ww.mall.common.exception.ApiException;
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
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
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

    private final SpelExpressionParser parser = new SpelExpressionParser();

    private static final String LOCK_PREFIX = "mall:lock";

    @Around("@annotation(com.ww.mall.redis.annotation.MallDistributedLock)")
    public Object mallDistributedLockAdvise(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String classDeclaringTypeName = signature.getDeclaringTypeName();
        Method method = signature.getMethod();
        // 获取方法参数名
        String[] parameterNames = new LocalVariableTableParameterNameDiscoverer().getParameterNames(method);
        // 获取方法参数
        Object[] parameterValues = joinPoint.getArgs();
        // 构建SpEL上下文，并设置变量值
        MyStandardEvaluationContext elContext = new MyStandardEvaluationContext(parameterNames, parameterValues);
        MallDistributedLock mallDistributedLock = method.getAnnotation(MallDistributedLock.class);
        StringBuilder sb = new StringBuilder(128);
        sb.append(LOCK_PREFIX).append(Constant.SPLIT).append(
                StringUtils.isNotEmpty(mallDistributedLock.value()) ?
                parser.parseExpression(mallDistributedLock.value()).getValue(elContext) : classDeclaringTypeName + Constant.SPLIT + method.getName()
        );
        if (StringUtils.isNotEmpty(mallDistributedLock.userId())) {
            sb.append(Constant.SPLIT).append(parser.parseExpression(mallDistributedLock.userId()).getValue(elContext));
        }
        if (StringUtils.isNotEmpty(mallDistributedLock.operationKey())) {
            sb.append(Constant.SPLIT).append(parser.parseExpression(mallDistributedLock.operationKey()).getValue(elContext));
        }
        String lockKey = sb.toString();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            log.info("线程【{}】尝试获取锁key：{}", Thread.currentThread().getId(), lockKey);
            boolean successGetLock = lock.tryLock(mallDistributedLock.waitTime(), mallDistributedLock.leaseTime(), mallDistributedLock.timeUnit());
            if (!successGetLock) {
                throw new ApiException(CodeEnum.FAIL_GET_LOCK_EXCEPTION.getCode(), CodeEnum.FAIL_GET_LOCK_EXCEPTION.getMessage());
            }
            log.info("线程【{}】获取到锁key：{}", Thread.currentThread().getId(), lockKey);
            return joinPoint.proceed();
        } catch (ApiException e) {
            throw new ApiException(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.info("线程【{}】锁key【{}】业务异常【{}】", Thread.currentThread().getId(), lockKey, e.getMessage());
            throw new ApiException(CodeEnum.LOCK_SERVICE_EXCEPTION.getCode(), CodeEnum.LOCK_SERVICE_EXCEPTION.getMessage());
        } finally {
            if (lock.isHeldByCurrentThread()) {
                log.info("线程【{}】释放锁key：{}", Thread.currentThread().getId(), lockKey);
                lock.unlock();
            }
        }
    }

    static class MyStandardEvaluationContext extends StandardEvaluationContext {
        public MyStandardEvaluationContext(String[] parameterNames, Object[] parameterValues) {
            if (parameterNames == null) {
                return;
            }
            for (int i = 0; i < parameterNames.length; i++) {
                String paramName = parameterNames[i];
                Object paramValue = parameterValues[i];
                setVariable(paramName, paramValue);
            }
        }
    }
}
