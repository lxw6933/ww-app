package com.ww.mall.order;

import com.ww.mall.annotation.enable.EnableMallRabbitmq;
import com.ww.mall.mybatis.annotation.EnableMallMybatis;
import com.ww.mall.redis.annotation.EnableMallRedis;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallRedis
@EnableMallRabbitmq
@EnableMallMybatis
@EnableDiscoveryClient
@SpringBootApplication
public class MallOrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallOrderApplication.class, args);
    }

}
