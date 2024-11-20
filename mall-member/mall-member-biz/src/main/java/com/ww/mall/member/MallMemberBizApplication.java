package com.ww.mall.member;

import com.ww.mall.mongodb.annotation.EnableMallMongodb;
import com.ww.mall.mybatis.annotation.EnableMallMybatis;
import com.ww.mall.rabbitmq.annotation.EnableMallRabbitmq;
import com.ww.mall.redis.annotation.EnableMallRedis;
import com.ww.mall.redis.annotation.EnableMallRedisson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallRedis
@EnableMallRedisson
@EnableMallRabbitmq
@EnableMallMybatis
@EnableMallMongodb
@EnableDiscoveryClient
@SpringBootApplication
public class MallMemberBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallMemberBizApplication.class, args);
    }

}
