package com.ww.mall.auth;

import com.ww.mall.annotation.enable.EnableMallMongodb;
import com.ww.mall.redis.annotation.EnableMallRedis;
import com.ww.mall.security.annotation.EnableMallSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallRedis
@EnableMallMongodb
@EnableMallSecurity
@EnableDiscoveryClient
@SpringBootApplication
public class MallAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallAuthApplication.class, args);
    }

}
