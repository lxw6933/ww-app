package com.ww.mall.admin;

import com.ww.mall.annotation.enable.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallSecurity
@EnableMallRedis
@EnableMallMongodb
@EnableMallRedisson
@EnableMallMybatisPlus
@EnableDiscoveryClient
@SpringBootApplication
public class MallAdminManageApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallAdminManageApplication.class, args);
    }

}
