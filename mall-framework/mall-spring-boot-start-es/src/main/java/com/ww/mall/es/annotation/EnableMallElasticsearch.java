package com.ww.mall.es.annotation;

import com.ww.mall.es.config.MallElasticsearchAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ww
 * @create 2023-07-15- 17:32
 * @description:
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({MallElasticsearchAutoConfiguration.class})
public @interface EnableMallElasticsearch {
}
