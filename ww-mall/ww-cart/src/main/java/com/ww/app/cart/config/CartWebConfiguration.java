package com.ww.app.cart.config;

import com.ww.app.cart.interceptor.CartInterceptor;
import com.ww.app.web.config.WebMvcConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/17 20:44
 **/
@Configuration
public class CartWebConfiguration extends WebMvcConfiguration {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CartInterceptor()).addPathPatterns("/**");
    }
}
