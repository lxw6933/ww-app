package com.ww.mall.admin;

import com.ww.mall.mongodb.annotation.EnableMallMongodb;
import com.ww.mall.mybatis.annotation.EnableMallMybatis;
import com.ww.mall.redis.annotation.EnableMallRedis;
import com.ww.mall.redis.annotation.EnableMallRedisson;
import com.ww.mall.security.annotation.EnableMallSecurity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallSecurity
@EnableMallRedis
@EnableMallMongodb
@EnableMallRedisson
@EnableMallMybatis
@EnableDiscoveryClient
@SpringBootApplication
public class MallAdminManageApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallAdminManageApplication.class, args);
    }

}
