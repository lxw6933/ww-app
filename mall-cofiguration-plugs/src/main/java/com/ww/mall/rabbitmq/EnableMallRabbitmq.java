package com.ww.mall.rabbitmq;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/15 22:11
 **/
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({MallRabbitmqAutoConfiguration.class})
public @interface EnableMallRabbitmq {
}
