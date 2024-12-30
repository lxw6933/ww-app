package com.ww.app.security.annotation;

import com.ww.app.security.component.AclComponent;
import com.ww.app.security.config.SecurityAutoConfiguration;
import com.ww.app.security.handler.AppAccessDeniedHandler;
import com.ww.app.security.handler.AppAuthenticationEntryPoint;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ww
 * @create 2024-09-21 11:07
 * @description:
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({AclComponent.class,
        AppAccessDeniedHandler.class,
        AppAuthenticationEntryPoint.class,
        SecurityAutoConfiguration.class})
public @interface EnableAppSecurity {
}
