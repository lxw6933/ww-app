package com.ww.mall.open;

import com.ww.mall.mongodb.annotation.EnableMallMongodb;
import com.ww.mall.mybatis.annotation.EnableMallMybatis;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallMongodb
@EnableMallMybatis
@EnableDiscoveryClient
@SpringBootApplication
public class MallOpenPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallOpenPlatformApplication.class, args);
    }

}
