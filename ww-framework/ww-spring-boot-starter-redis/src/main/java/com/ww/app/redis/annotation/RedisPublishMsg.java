package com.ww.app.redis.annotation;

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
public @interface RedisPublishMsg {

    /**
     * 发布订阅渠道名称
     */
    String value() default "";

    /**
     * 发布消息
     */
    String message() default "";

    /**
     * 用户消息【直接获取当前c端用户id作为消息】
     */
    boolean userMsgFlag() default false;

}
