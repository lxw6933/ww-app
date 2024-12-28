package com.ww.mall.admin;

import com.ww.mall.mongodb.annotation.EnableAppMongodb;
import com.ww.mall.mybatis.annotation.EnableAppMybatis;
import com.ww.mall.redis.annotation.EnableAppRedis;
import com.ww.mall.redis.annotation.EnableAppRedisson;
import com.ww.mall.security.annotation.EnableAppSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableAppSecurity
@EnableAppRedis
@EnableAppMongodb
@EnableAppRedisson
@EnableAppMybatis
@EnableDiscoveryClient
@SpringBootApplication
public class AdminManageServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminManageServerApplication.class, args);
    }

}
