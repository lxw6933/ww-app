package com.ww.mall.web.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.SentinelWebInterceptor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.ww.mall.web.interceptor.RequestInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

/**
 * @author ww
 * @create 2023-07-14- 18:41
 * @description:
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
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
        /**
         * String 返回异常处理
         */
        // 第一种方式是将 json 处理的转换器放到第一位，使得先让 json 转换器处理返回值，这样 String转换器就处理不了了。
//        converters.add(0, new MappingJackson2HttpMessageConverter());
        // 第二种就是把String类型的转换器去掉，不使用String类型的转换器
        converters.removeIf(httpMessageConverter -> httpMessageConverter.getClass() == StringHttpMessageConverter.class);
        // 处理BigDecimal返回前端格式化问题
        for (HttpMessageConverter<?> converter : converters) {
            if(converter instanceof MappingJackson2HttpMessageConverter){
                SimpleModule simpleModule = new SimpleModule();
                simpleModule.addSerializer(BigDecimal.class, new BigDecimalSerializer());
                ((MappingJackson2HttpMessageConverter) converter).getObjectMapper().registerModule(simpleModule);
                break;
            }
        }
    }

    public static class BigDecimalSerializer extends JsonSerializer<BigDecimal> {
        private final DecimalFormat decimalFormat = new DecimalFormat("0.00");
        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value != null) {
                gen.writeString(decimalFormat.format(value));
            }
        }
    }
}
