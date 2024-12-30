package com.ww.app.web.annotation;

import com.ww.app.web.validator.XssValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ww
 * @create 2024-11-04- 15:34
 * @description:
 */
@Target(value = { ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = XssValidator.class)
public @interface Xss {

    String message() default "非法输入, 检测到潜在的xss代码";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

