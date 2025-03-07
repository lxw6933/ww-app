package com.ww.app.web.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.SentinelWebInterceptor;
import com.ww.app.common.utils.json.JacksonUtils;
import com.ww.app.web.interceptor.RequestInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2023-07-14- 18:41
 * @description:
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置 favicon.ico 的路径
        registry.addResourceHandler("/favicon.ico").addResourceLocations("classpath:/");
        // 配置 Knife4j 的 doc.html 路径
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        // 配置外部jar
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/")
        // 设置缓存策略
                .setCacheControl(CacheControl.maxAge(5, TimeUnit.HOURS).cachePublic());
    }

    @Override
    public void configurePathMatch(@NonNull PathMatchConfigurer configurer) {
        WebMvcConfigurer.super.configurePathMatch(configurer);
        // 设置uri最后的/不匹配
        configurer.setUseTrailingSlashMatch(false);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SentinelWebInterceptor());
        registry.addInterceptor(new RequestInterceptor()).addPathPatterns("/**");
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // String 返回异常处理
        // 第一种方式是将 json 处理的转换器放到第一位，使得先让 json 转换器处理返回值，这样 String转换器就处理不了了。
        // jackson放在第二位，防止knife4j是byte序列化导致无法加载文档
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter.setObjectMapper(JacksonUtils.getObjectMapper());
        converters.add(1, mappingJackson2HttpMessageConverter);
        // 第二种就是把String类型的转换器去掉，不使用String类型的转换器 [会导致prometheus数据异常]
//        converters.removeIf(httpMessageConverter -> httpMessageConverter.getClass() == StringHttpMessageConverter.class);
    }

}
