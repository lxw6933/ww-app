package com.ww.mall.product;

import com.ww.mall.rabbitmq.EnableMallRabbitmq;
import com.ww.mall.redis.EnableMallRedis;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

@EnableMallRedis
@EnableMallRabbitmq
@EnableDiscoveryClient
@SpringBootApplication
@MapperScan("com.ww.mall.product.dao")
public class MallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallProductApplication.class, args);
    }

}
