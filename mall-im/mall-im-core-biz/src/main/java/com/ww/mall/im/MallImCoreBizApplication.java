package com.ww.mall.im;

import com.ww.mall.redis.annotation.EnableMallRedis;
import com.ww.mall.redis.annotation.EnableMallRedisson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallRedis
@EnableMallRedisson
@EnableDiscoveryClient
@SpringBootApplication
public class MallImCoreBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallImCoreBizApplication.class, args);
    }

}
