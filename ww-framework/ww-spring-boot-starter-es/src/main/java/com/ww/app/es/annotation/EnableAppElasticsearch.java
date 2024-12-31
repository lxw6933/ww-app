package com.ww.app.es.annotation;

import com.ww.app.es.config.ElasticsearchAutoConfiguration;
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
@Deprecated
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({ElasticsearchAutoConfiguration.class})
public @interface EnableAppElasticsearch {
}
