package com.ww.app.sensitive.annotation;

import com.ww.app.sensitive.config.SensitiveWordAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ww
 * @create 2024-05-24- 18:05
 * @description:
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({SensitiveWordAutoConfiguration.class})
public @interface EnableAppSensitiveWord {
}
