package com.ww.app.auth;

import com.ww.app.mongodb.annotation.EnableAppMongodb;
import com.ww.app.redis.annotation.EnableAppRedis;
import com.ww.app.security.annotation.EnableAppSecurity;
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
