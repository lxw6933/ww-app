package com.ww.mall.redis.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author ww
 * @create 2023-09-05- 13:59
 * @description: 限流注解
 */
@Component
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MallRateLimit {



}
