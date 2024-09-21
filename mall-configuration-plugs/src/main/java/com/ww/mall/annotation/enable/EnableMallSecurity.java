package com.ww.mall.annotation.enable;

import com.ww.mall.security.MallSecurityAutoConfiguration;
import com.ww.mall.security.component.AclComponent;
import com.ww.mall.security.handler.MallAccessDeniedHandler;
import com.ww.mall.security.handler.MallAuthenticationEntryPoint;
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
        MallAccessDeniedHandler.class,
        MallAuthenticationEntryPoint.class,
        MallSecurityAutoConfiguration.class})
public @interface EnableMallSecurity {
}
