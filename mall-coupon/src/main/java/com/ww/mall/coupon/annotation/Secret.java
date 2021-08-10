package com.ww.mall.coupon.annotation;

import java.lang.annotation.*;

/**
 * @description: 参数加密注解
 * @author: ww
 * @create: 2021-06-07 10:02
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Secret {

    /**
     * 是否加密
     *
     * @return boolean
     */
    boolean encode() default true;

    /**
     * 是否解密
     *
     * @return boolean
     */
    boolean decode() default true;
}
