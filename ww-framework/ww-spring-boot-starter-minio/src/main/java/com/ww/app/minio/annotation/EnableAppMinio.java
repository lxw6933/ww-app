package com.ww.app.minio.annotation;

import com.ww.app.minio.config.MinioAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Deprecated
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({MinioAutoConfiguration.class})
public @interface EnableAppMinio {
}
