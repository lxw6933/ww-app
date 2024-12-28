package com.ww.mall.redis.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2023-09-05- 10:35
 * @description: 防重复提交注解
 */
@Component
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Resubmission {

    /**
     * 过期秒数,默认为3秒
     */
    long expire() default 3;

    /**
     * 超时时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

}
