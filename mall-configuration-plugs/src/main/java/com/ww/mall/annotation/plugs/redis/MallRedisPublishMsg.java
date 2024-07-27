package com.ww.mall.annotation.plugs.redis;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author ww
 * @create 2023-11-30- 13:48
 * @description:
 */
@Component
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MallRedisPublishMsg {

    /**
     * 发布订阅渠道名称
     */
    String value() default "";

    /**
     * 发布消息
     */
    String message() default "";

}
