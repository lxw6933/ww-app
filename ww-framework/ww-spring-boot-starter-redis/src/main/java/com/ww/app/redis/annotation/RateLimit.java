package com.ww.app.redis.annotation;

import java.lang.annotation.*;

/**
 * @author ww
 * @create 2023-09-05- 13:59
 * @description: 限流注解
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * 限流类型，用于区分不同的限流策略
     */
    String type() default "";
    
    /**
     * 时间窗口大小(秒)
     */
    int period() default 1;
    
    /**
     * 时间窗口内最大请求数
     */
    int count() default 100;
    
    /**
     * 限流提示消息
     */
    String message() default "当前访问人数过多，请稍后再试";
    
    /**
     * 限流粒度
     * IP: 按IP限流
     * USER: 按用户限流
     * IP_USER: 按IP和用户组合限流
     * METHOD: 按方法限流（默认）
     */
    LimitType limitType() default LimitType.METHOD;
    
    /**
     * 是否启用白名单
     */
    boolean enableWhitelist() default false;
    
    /**
     * 是否启用黑名单
     */
    boolean enableBlacklist() default false;
    
    /**
     * 限流降级策略
     * NONE: 不降级，直接抛出异常
     * CACHE: 返回缓存数据
     * FALLBACK: 执行降级方法
     */
    FallbackStrategy fallbackStrategy() default FallbackStrategy.NONE;
    
    /**
     * 降级方法名，仅在fallbackStrategy为FALLBACK时有效
     */
    String fallbackMethod() default "";
    
    /**
     * 限流粒度枚举
     */
    enum LimitType {
        IP, USER, IP_USER, METHOD
    }
    
    /**
     * 降级策略枚举
     */
    enum FallbackStrategy {
        NONE, CACHE, FALLBACK
    }
}
