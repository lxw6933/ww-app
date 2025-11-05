package com.ww.app.cart.aspect;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 购物车指标统计切面
 * 使用AOP解耦业务代码和监控指标
 *
 * @author ww
 * @date 2025-11-05
 */
@Slf4j
@Aspect
@Component
public class CartMetricsAspect {

    @Resource
    private MeterRegistry meterRegistry;

    // 性能指标
    private Counter addCartCounter;
    private Counter deleteCartCounter;
    private Timer queryCartTimer;

    @PostConstruct
    public void init() {
        // 初始化指标统计（如果MeterRegistry可用）
        if (meterRegistry != null) {
            addCartCounter = Counter.builder("cart.add")
                    .description("购物车添加操作次数")
                    .register(meterRegistry);
            deleteCartCounter = Counter.builder("cart.delete")
                    .description("购物车删除操作次数")
                    .register(meterRegistry);
            queryCartTimer = Timer.builder("cart.query")
                    .description("购物车查询耗时")
                    .register(meterRegistry);
            
            log.info("购物车指标统计切面初始化完成");
        } else {
            log.warn("MeterRegistry未配置，指标统计功能将不可用");
        }
    }

    /**
     * 统计添加购物车操作
     */
    @Around("execution(* com.ww.app.cart.service.HashCartService.addToCart(..))")
    public Object aroundAddToCart(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        incrementCounter(addCartCounter);
        return result;
    }

    /**
     * 统计删除操作
     */
    @Around("execution(* com.ww.app.cart.service.HashCartService.deleteItem(..)) || " +
            "execution(* com.ww.app.cart.service.HashCartService.batchDeleteItem(..)) || " +
            "execution(* com.ww.app.cart.service.HashCartService.clearUserCart(..))")
    public Object aroundDelete(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        incrementCounter(deleteCartCounter);
        return result;
    }

    /**
     * 统计查询购物车耗时
     */
    @Around("execution(* com.ww.app.cart.service.HashCartService.userCartList(..))")
    public Object aroundQuery(ProceedingJoinPoint joinPoint) throws Throwable {
        if (queryCartTimer != null) {
            Timer.Sample sample = Timer.start();
            try {
                return joinPoint.proceed();
            } finally {
                sample.stop(queryCartTimer);
            }
        }
        return joinPoint.proceed();
    }

    /**
     * 增加计数器（如果可用）
     */
    private void incrementCounter(Counter counter) {
        if (counter != null) {
            counter.increment();
        }
    }
}
