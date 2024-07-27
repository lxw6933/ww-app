package com.ww.mall.annotation.enable;

import com.ww.mall.sensitive.MallSensitiveWordAutoConfiguration;
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
@Import({MallSensitiveWordAutoConfiguration.class})
public @interface EnableMallSensitiveWord {
}
