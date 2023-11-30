package com.ww.mall.redis.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2023-11-30- 13:48
 * @description:
 */
@Component
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MallDistributedLock {

    /**
     * 锁前缀 默认方法名
     */
    String prefixKey() default "";

    /**
     * 用户id key锁
     */
    String userId() default "";

    /**
     * 业务锁
     */
    String operationKey() default "";

    /**
     * 获取锁等待时间
     */
    int waitTime() default 5;

    /**
     * 锁释放时间
     */
    int leaseTime() default 10;

    /**
     * 锁时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

}
