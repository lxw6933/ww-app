package com.ww.app.member;

import com.ww.app.mongodb.annotation.EnableAppMongodb;
import com.ww.app.mybatis.annotation.EnableAppMybatis;
import com.ww.app.rabbitmq.annotation.EnableAppRabbitmq;
import com.ww.app.redis.annotation.EnableAppRedis;
import com.ww.app.redis.annotation.EnableAppRedisson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableAppRedis
@EnableAppRedisson
@EnableAppRabbitmq
@EnableAppMybatis
@EnableAppMongodb
@EnableDiscoveryClient
@SpringBootApplication
public class MemberServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemberServerApplication.class, args);
    }

}
