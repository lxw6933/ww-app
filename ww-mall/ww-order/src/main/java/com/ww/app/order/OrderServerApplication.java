package com.ww.app.order;

import com.ww.app.mybatis.annotation.EnableAppMybatis;
import com.ww.app.rabbitmq.annotation.EnableAppRabbitmq;
import com.ww.app.redis.annotation.EnableAppRedis;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableAppRedis
@EnableAppRabbitmq
@EnableAppMybatis
@EnableDiscoveryClient
@SpringBootApplication
public class OrderServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServerApplication.class, args);
    }

}
