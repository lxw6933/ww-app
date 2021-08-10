package com.ww.mall.config;

import com.ww.mall.interceptor.AuthorizationInterceptor;
import com.ww.mall.resolver.CurrentUserMethodArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.util.List;

/**
 * @description: web 配置
 * 拦截器配置
 * void addInterceptors(InterceptorRegistry var1);
 * 视图跳转控制器
 * void addViewControllers(ViewControllerRegistry registry);
 * 静态资源处理
 * void addResourceHandlers(ResourceHandlerRegistry registry);
 * 默认静态资源处理器
 * void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer);
 * 配置视图解析器
 * void configureViewResolvers(ViewResolverRegistry registry);
 * 配置内容裁决的一些选项
 * void configureContentNegotiation(ContentNegotiationConfigurer configurer);
 * 解决跨域问题
 * void addCorsMappings(CorsRegistry registry);
 * @author: ww
 * @create: 2021-05-17 16:39
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private CurrentUserMethodArgumentResolver currentUserMethodArgumentResolver;

    @Resource
    private AuthorizationInterceptor authorizationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizationInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(currentUserMethodArgumentResolver);
    }

}
