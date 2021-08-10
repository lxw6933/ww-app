package com.ww.mall.annotation;

import com.ww.mall.enums.Action;

import java.lang.annotation.*;

/**
 * @description: 系统日志注解
 * @author: ww
 * @create: 2021-05-12 19:33
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SysLog {

    String value() default "";

    /**
     * 日志类型
     */
    int type() default 0;

    /**
     * 动作
     */
    Action action() default Action.NORMAL;
}
