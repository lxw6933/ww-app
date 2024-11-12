package com.ww.mall.product;

import com.ww.mall.mybatis.annotation.EnableMallMybatis;
import com.ww.mall.rabbitmq.annotation.EnableMallRabbitmq;
import com.ww.mall.redis.annotation.EnableMallRedis;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallRedis
@EnableMallRabbitmq
@EnableMallMybatis
@EnableDiscoveryClient
@SpringBootApplication
public class MallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallProductApplication.class, args);
    }

}
