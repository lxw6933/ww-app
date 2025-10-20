package com.ww.app.web.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2025-10-20 14:03
 * @description:
 */
@Slf4j
@Aspect
@Component
public class TimeCostAspect {

    /**
     * 同时支持方法级与类级的 TimeCost 注解
     */
    @Around("@annotation(com.ww.app.web.annotation.TimeCost) || @within(com.ww.app.web.annotation.TimeCost)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTimeNs = System.nanoTime();

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Class<?> targetClass = methodSignature.getDeclaringType();
        String methodName = methodSignature.getName();
        String className = targetClass.getSimpleName();

        log.info("[TimeCost] 开始执行 {}#{}", className, methodName);

        try {
            Object result = joinPoint.proceed();
            long costNs = System.nanoTime() - startTimeNs;
            long costMs = TimeUnit.NANOSECONDS.toMillis(costNs);
            Duration duration = Duration.ofNanos(costNs);

            // 超过阈值则以 warn 打印（默认 1s 阈值）
            long warnThresholdMs = 1000L;
            if (costMs >= warnThresholdMs) {
                log.warn("[TimeCost] 结束 {}#{} 耗时: {} ms (~{} s)", className, methodName, costMs, duration.getSeconds());
            } else {
                log.info("[TimeCost] 结束 {}#{} 耗时: {} ms", className, methodName, costMs);
            }
            return result;
        } catch (Throwable ex) {
            long costNs = System.nanoTime() - startTimeNs;
            long costMs = TimeUnit.NANOSECONDS.toMillis(costNs);
            log.error("[TimeCost] 异常 {}#{} 耗时: {} ms, 异常: {}", className, methodName, costMs, ex.toString());
            throw ex;
        }
    }

}
