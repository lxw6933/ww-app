package com.ww.mall.auth;

import com.ww.mall.annotation.enable.EnableMallRedis;
import com.ww.mall.annotation.enable.EnableMallSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallRedis
@EnableMallSecurity
@EnableDiscoveryClient
@SpringBootApplication
public class MallAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallAuthApplication.class, args);
    }

}
