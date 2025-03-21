package com.ww.app.redis.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2023-09-05- 10:40
 * @description:
 */
@Aspect
@Component
public class RateLimitAspect {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Around("@annotation(com.ww.app.redis.annotation.RateLimit)")
    public Object mallRateLimitAdvice(ProceedingJoinPoint joinPoint) throws Throwable {

        return joinPoint.proceed();
    }

}
