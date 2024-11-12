package com.ww.mall.coupon;

import com.ww.mall.annotation.enable.*;
import com.ww.mall.mybatis.annotation.EnableMallMybatis;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallRedis
@EnableMallMongodb
@EnableMallRabbitmq
@EnableMallRedisson
@EnableMallMybatis
@EnableDiscoveryClient
@SpringBootApplication
public class MallCouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallCouponApplication.class, args);
    }

}
