package com.ww.mall.web;

import com.ww.mall.web.handler.RequestBodyHandler;
import com.ww.mall.web.handler.ResExceptionHandler;
import com.ww.mall.web.handler.ResponseBodyHandler;
import com.ww.mall.web.handler.ServerSentinelHandler;
import com.ww.mall.web.interceptor.FeignRequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2023-07-14- 18:45
 * @description:
 */
@Slf4j
@Configuration
public class MallWebApplication {

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
        log.info("初始化RequestBodyHandler成功...");
        return new RequestBodyHandler();
    }

    @Bean
    public ResponseBodyHandler responseBodyHandler() {
        log.info("初始化ResponseBodyHandler成功...");
        return new ResponseBodyHandler();
    }

}
