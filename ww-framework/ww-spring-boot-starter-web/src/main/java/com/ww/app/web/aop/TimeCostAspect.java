package com.ww.app.web.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * @author ww
 * @create 2025-10-20 14:03
 * @description:
 */
@Slf4j
@Aspect
@Component
public class TimeCostAspect {

    @Around("@annotation(com.ww.app.web.annotation.TimeCost)")
    public Object exportExcel(ProceedingJoinPoint joinPoint) {
        long startTime = System.nanoTime();
        log.info("开始：{}", joinPoint.getSignature().getName());
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            long cost = System.nanoTime() - startTime;
            Duration time = Duration.ofNanos(cost);
            log.info("结束，消耗了：{}s  {}ms", time.getSeconds(), cost);
        }
    }

}
