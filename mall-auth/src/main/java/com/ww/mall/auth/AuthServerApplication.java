package com.ww.mall.auth;

import com.ww.mall.mongodb.annotation.EnableAppMongodb;
import com.ww.mall.redis.annotation.EnableAppRedis;
import com.ww.mall.security.annotation.EnableAppSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableAppRedis
@EnableAppMongodb
@EnableAppSecurity
@EnableDiscoveryClient
@SpringBootApplication
public class AuthServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServerApplication.class, args);
    }

}
