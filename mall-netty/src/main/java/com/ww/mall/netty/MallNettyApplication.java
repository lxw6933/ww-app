package com.ww.mall.netty;

import com.ww.mall.annotation.enable.EnableMallMongodb;
import com.ww.mall.annotation.enable.EnableMallRedis;
import com.ww.mall.annotation.enable.EnableMallRedisson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallRedis
@EnableMallRedisson
@EnableMallMongodb
@EnableDiscoveryClient
@SpringBootApplication
public class MallNettyApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallNettyApplication.class, args);
    }

}
