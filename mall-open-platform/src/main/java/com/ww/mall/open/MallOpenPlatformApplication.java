package com.ww.mall.open;

import com.ww.mall.annotation.enable.EnableMallMongodb;
import com.ww.mall.annotation.enable.EnableMallMybatisPlus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallMongodb
@EnableMallMybatisPlus
@EnableDiscoveryClient
@SpringBootApplication
public class MallOpenPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallOpenPlatformApplication.class, args);
    }

}
