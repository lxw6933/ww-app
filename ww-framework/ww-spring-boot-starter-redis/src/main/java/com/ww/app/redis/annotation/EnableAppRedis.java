package com.ww.app.redis.annotation;

import com.ww.app.redis.config.LuaScriptConfig;
import com.ww.app.redis.config.RedisAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ww
 * @create 2023-07-15- 15:18
 * @description:
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({RedisAutoConfiguration.class, LuaScriptConfig.class})
public @interface EnableAppRedis {
}
