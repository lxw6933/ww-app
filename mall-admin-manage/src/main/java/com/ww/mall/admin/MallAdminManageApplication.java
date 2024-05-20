package com.ww.mall.admin;

import com.ww.mall.mongodb.EnableMallMongodb;
import com.ww.mall.rabbitmq.EnableMallRabbitmq;
import com.ww.mall.redis.EnableMallRedis;
import com.ww.mall.redis.EnableMallRedisson;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallRedis
@EnableMallMongodb
@EnableMallRedisson
@EnableDiscoveryClient
@SpringBootApplication
@MapperScan("com.ww.mall.admin.dao")
public class MallAdminManageApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallAdminManageApplication.class, args);
    }

}
