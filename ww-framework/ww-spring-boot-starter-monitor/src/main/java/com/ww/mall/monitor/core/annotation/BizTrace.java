package com.ww.mall.monitor.core.annotation;

import java.lang.annotation.*;

/**
 * 统一业务追踪注解：在关键方法上打标记，便于 SkyWalking 中细粒度观测
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BizTrace {

    /**
     * 业务操作名；为空则默认使用 方法签名
     */
    String operation() default "";

    /**
     * 是否记录入参（字符串化，长度有限制）
     */
    boolean includeArgs() default true;

    /**
     * 是否记录返回值（字符串化，长度有限制）
     */
    boolean includeReturn() default true;

    /**
     * 是否记录用户信息（userId、userType 等）
     */
    boolean includeUser() default true;

}


