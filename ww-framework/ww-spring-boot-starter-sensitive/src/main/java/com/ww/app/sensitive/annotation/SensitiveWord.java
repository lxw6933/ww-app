package com.ww.app.sensitive.annotation;

import com.ww.app.common.enums.SensitiveWordHandlerType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ww
 * @create 2024-05-24 21:03
 * @description:
 */
@Target(value = {ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface SensitiveWord {

    /**
     * 内容
     */
    String[] content();

    /**
     * 过滤类型
     */
    SensitiveWordHandlerType handlerType() default SensitiveWordHandlerType.EXCEPTION;

    /**
     * 异常提示语
     */
    String msg() default "内容存在非法字符";

}

