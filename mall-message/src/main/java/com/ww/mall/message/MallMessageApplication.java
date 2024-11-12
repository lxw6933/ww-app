package com.ww.mall.message;

import com.ww.mall.annotation.enable.*;
import com.ww.mall.redis.annotation.EnableMallRedis;
import com.ww.mall.redis.annotation.EnableMallRedisson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

//@EnableMallXxlJob
@EnableMallRedis
@EnableMallRedisson
@EnableMallMongodb
@EnableMallRabbitmq
@EnableDiscoveryClient
@SpringBootApplication
public class MallMessageApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallMessageApplication.class, args);
    }

}
