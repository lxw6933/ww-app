package com.ww.mall.open;

import com.ww.mall.mongodb.annotation.EnableAppMongodb;
import com.ww.mall.mybatis.annotation.EnableAppMybatis;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableAppMongodb
@EnableAppMybatis
@EnableDiscoveryClient
@SpringBootApplication
public class OpenPlatformServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenPlatformServerApplication.class, args);
    }

}
