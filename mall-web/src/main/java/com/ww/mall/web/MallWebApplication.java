package com.ww.mall.web;

import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.ww.mall.web.config.LoadBalancerConfiguration;
import com.ww.mall.web.handler.RequestBodyHandler;
import com.ww.mall.web.handler.ResExceptionHandler;
import com.ww.mall.web.handler.ResponseBodyHandler;
import com.ww.mall.web.handler.ServerSentinelHandler;
import com.ww.mall.web.interceptor.FeignRequestInterceptor;
import io.github.linpeilie.annotations.ComponentModelConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @author ww
 * @create 2023-07-14- 18:45
 * @description:
 */
@Slf4j
@EnableRetry
@Configuration
@ComponentModelConfig(componentModel = "default")
@EnableFeignClients(basePackages = "com.ww.mall.web.feign")
@LoadBalancerClients(defaultConfiguration = {LoadBalancerConfiguration.class})
public class MallWebApplication {

//    @Bean
//    @Primary
//    public DataSourceTransactionManager dataSourceTransactionManager(DataSource dataSource) {
//        // 如果引入mongodb事务管理器，这个bean必须存在，否则可以不需要
//        log.info("初始化mysql的默认事务管理器...");
//        return new DataSourceTransactionManager(dataSource);
//    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        log.info("初始化jackson自定义序列化成功...");
        return jacksonObjectMapperBuilder -> {
            // region 注册序列化器
            jacksonObjectMapperBuilder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(DatePattern.NORM_DATETIME_FORMATTER));
            jacksonObjectMapperBuilder.serializerByType(LocalDate.class, new LocalDateSerializer(DatePattern.NORM_DATE_FORMATTER));
            jacksonObjectMapperBuilder.serializerByType(LocalTime.class, new LocalTimeSerializer(DatePattern.NORM_TIME_FORMATTER));
            // region 注册反序列化器
            jacksonObjectMapperBuilder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(DatePattern.NORM_DATETIME_FORMATTER));
            jacksonObjectMapperBuilder.deserializerByType(LocalDate.class, new LocalDateDeserializer(DatePattern.NORM_DATE_FORMATTER));
            jacksonObjectMapperBuilder.deserializerByType(LocalTime.class, new LocalTimeDeserializer(DatePattern.NORM_TIME_FORMATTER));
        };
    }

    @Bean
    public FeignRequestInterceptor feignRequestInterceptor() {
        log.info("初始化feign远程调用拦截器FeignRequestInterceptor成功...");
        return new FeignRequestInterceptor();
    }

    @Bean
    public ServerSentinelHandler serverSentinelConfiguration() {
        log.info("初始化服务限流处理器ServerSentinelConfiguration成功...");
        return new ServerSentinelHandler();
    }

    @Bean
    public ResExceptionHandler resExceptionHandler() {
        log.info("初始化全局异常处理器ResExceptionHandler成功...");
        return new ResExceptionHandler();
    }

    @Bean
    public RequestBodyHandler requestBodyHandler() {
        return new RequestBodyHandler();
    }

    @Bean
    public ResponseBodyHandler responseBodyHandler() {
        return new ResponseBodyHandler();
    }

}
