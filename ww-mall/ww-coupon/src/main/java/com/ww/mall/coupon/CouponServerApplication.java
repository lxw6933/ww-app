package com.ww.mall.coupon;

import com.ww.mall.mongodb.annotation.EnableAppMongodb;
import com.ww.mall.mybatis.annotation.EnableAppMybatis;
import com.ww.mall.rabbitmq.annotation.EnableAppRabbitmq;
import com.ww.mall.redis.annotation.EnableAppRedis;
import com.ww.mall.redis.annotation.EnableAppRedisson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableAppRedis
@EnableAppMongodb
@EnableAppRabbitmq
@EnableAppRedisson
@EnableAppMybatis
@EnableDiscoveryClient
@SpringBootApplication
public class CouponServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CouponServerApplication.class, args);
    }

}
