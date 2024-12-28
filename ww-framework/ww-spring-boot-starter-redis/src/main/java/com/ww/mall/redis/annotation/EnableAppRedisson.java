package com.ww.mall.redis.annotation;

import com.ww.mall.redis.config.RedissonAutoConfig;
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
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({RedissonAutoConfig.class})
public @interface EnableAppRedisson {

}
