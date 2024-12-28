package com.ww.mall.redis.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2023-09-05- 10:40
 * @description:
 */
@Aspect
@Component
public class RateLimitAspect {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Around("@annotation(com.ww.mall.redis.annotation.RateLimit)")
    public Object mallRateLimitAdvice(ProceedingJoinPoint joinPoint) throws Throwable {

        return joinPoint.proceed();
    }

}
