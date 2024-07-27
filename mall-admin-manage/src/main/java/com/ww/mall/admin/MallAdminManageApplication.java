package com.ww.mall.admin;

import com.ww.mall.annotation.enable.EnableMallMongodb;
import com.ww.mall.annotation.enable.EnableMallRedis;
import com.ww.mall.annotation.enable.EnableMallRedisson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallRedis
@EnableMallMongodb
@EnableMallRedisson
@EnableDiscoveryClient
@SpringBootApplication
public class MallAdminManageApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallAdminManageApplication.class, args);
    }

}
