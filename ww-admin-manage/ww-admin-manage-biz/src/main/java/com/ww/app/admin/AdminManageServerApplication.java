package com.ww.app.admin;

import com.ww.app.mongodb.annotation.EnableAppMongodb;
import com.ww.app.mybatis.annotation.EnableAppMybatis;
import com.ww.app.redis.annotation.EnableAppRedis;
import com.ww.app.redis.annotation.EnableAppRedisson;
import com.ww.app.security.annotation.EnableAppSecurity;
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
