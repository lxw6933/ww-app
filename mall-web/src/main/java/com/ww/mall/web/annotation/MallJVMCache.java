package com.ww.mall.web.annotation;

import org.apache.poi.ss.formula.functions.T;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ww
 * @create 2024-04-07- 18:14
 * @description:
 */
@Component
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MallJVMCache {

    String key();

    Object value();

    Class classType();

}
