package com.ww.mall.client;

import com.ww.mall.annotation.enable.EnableMallMongodb;
import com.ww.mall.annotation.enable.EnableMallWebSocket;
import com.ww.mall.annotation.enable.EnableMallRabbitmq;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallMongodb
@EnableMallRabbitmq
@EnableMallWebSocket
@EnableDiscoveryClient
@SpringBootApplication
public class MallGrpcClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallGrpcClientApplication.class, args);
    }

}
