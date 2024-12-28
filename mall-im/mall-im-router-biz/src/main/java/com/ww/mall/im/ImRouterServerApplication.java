package com.ww.mall.im;

import com.ww.mall.redis.annotation.EnableAppRedis;
import com.ww.mall.redis.annotation.EnableAppRedisson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableAppRedis
@EnableAppRedisson
@EnableDiscoveryClient
@SpringBootApplication
public class ImRouterServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImRouterServerApplication.class, args);
    }

}
