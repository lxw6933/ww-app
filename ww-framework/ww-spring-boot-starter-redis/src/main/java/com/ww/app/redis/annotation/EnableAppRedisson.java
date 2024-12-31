package com.ww.app.redis.annotation;

import com.ww.app.redis.config.RedissonAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ww
 * @create 2023-07-25- 15:15
 * @description:
 */
@Deprecated
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({RedissonAutoConfiguration.class})
public @interface EnableAppRedisson {

}
