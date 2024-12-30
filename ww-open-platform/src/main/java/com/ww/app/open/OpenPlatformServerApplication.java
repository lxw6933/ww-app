package com.ww.app.open;

import com.ww.app.mongodb.annotation.EnableAppMongodb;
import com.ww.app.mybatis.annotation.EnableAppMybatis;
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
