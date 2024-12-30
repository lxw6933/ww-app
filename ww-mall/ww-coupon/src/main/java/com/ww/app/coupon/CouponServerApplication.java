package com.ww.app.coupon;

import com.ww.app.mongodb.annotation.EnableAppMongodb;
import com.ww.app.mybatis.annotation.EnableAppMybatis;
import com.ww.app.rabbitmq.annotation.EnableAppRabbitmq;
import com.ww.app.redis.annotation.EnableAppRedis;
import com.ww.app.redis.annotation.EnableAppRedisson;
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
