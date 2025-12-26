package com.ww.app.open.config;

import com.ww.app.open.interceptor.OpenApiInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * 开放平台配置类
 * 
 * @author ww
 * @create 2024-05-27
 * @description: 配置开放平台相关的拦截器、路径等
 */
@Configuration
public class OpenApiConfig implements WebMvcConfigurer {

    @Resource
    private OpenApiInterceptor openApiInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(openApiInterceptor)
                .addPathPatterns("/open/api/**")  // 拦截所有开放平台API请求
                .excludePathPatterns(
                    "/open/api/health",           // 健康检查
                    "/open/api/docs/**",          // API文档
                    "/open/api/swagger/**"        // Swagger文档
                );
    }
}


