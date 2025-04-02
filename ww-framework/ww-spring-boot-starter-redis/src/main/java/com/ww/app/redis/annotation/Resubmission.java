package com.ww.app.redis.annotation;

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
     * 过期秒数,默认为5秒
     */
    long expire() default 5;

    /**
     * 超时时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 自定义key，支持简单表达式如 #p0.id-#p1
     * 优先级高于默认的方法签名+参数生成方式
     */
    String key() default "";

    /**
     * 指定参与计算key的参数索引
     * 为空时使用所有参数
     */
    int[] paramIndexes() default {};

    /**
     * 严格模式
     * true: Redis异常时直接拒绝请求
     * false: Redis异常时允许请求继续执行
     */
    boolean strictMode() default false;

}
