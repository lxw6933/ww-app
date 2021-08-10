package com.ww.mall.coupon.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @description: redis缓存注解
 * @author: ww
 * @create: 2021-05-24 16:28
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cache {

    /**
     * 缓存key
     * @return string
     */
    String key();

    /**
     * 是否需要md5 key
     * @return boolean
     */
    boolean mode() default true;

    /**
     * 缓存时间，默认一小时
     * @return long
     */
    long timeout() default 3600;

    /**
     * 时间单位，默认秒
     * @return TimeUnit
     */
    TimeUnit unit() default TimeUnit.SECONDS;

}
