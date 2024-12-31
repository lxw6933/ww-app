package com.ww.app.mongodb.annotation;

import com.ww.app.mongodb.config.MongodbAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ww
 * @create 2023-07-15- 16:15
 * @description:
 */
@Deprecated
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({MongodbAutoConfiguration.class})
public @interface EnableAppMongodb {
}
