package com.ww.app.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API安全验证注解
 * 标记需要进行签名验证的接口
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiSecured {
    
    /**
     * 是否启用时间戳验证，防止重放攻击
     */
    boolean enableTimestamp() default true;
    
    /**
     * 时间戳有效期(秒)
     */
    long timestampExpire() default 60;
}