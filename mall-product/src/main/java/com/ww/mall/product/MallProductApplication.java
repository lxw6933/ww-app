package com.ww.mall.product;

import com.ww.mall.rabbitmq.EnableMallRabbitmq;
import com.ww.mall.redis.EnableMallRedis;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallRedis
@EnableMallRabbitmq
@EnableDiscoveryClient
@SpringBootApplication
public class MallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallProductApplication.class, args);
    }

}
