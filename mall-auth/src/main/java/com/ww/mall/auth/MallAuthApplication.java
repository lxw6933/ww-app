package com.ww.mall.auth;

import com.ww.mall.redis.EnableMallRedis;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableMallRedis
@RefreshScope
@EnableDiscoveryClient
@SpringBootApplication
@MapperScan("com.ww.mall.auth.dao")
@EnableFeignClients(basePackages = "com.ww.mall.auth.feign")
public class MallAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallAuthApplication.class, args);
    }

}
