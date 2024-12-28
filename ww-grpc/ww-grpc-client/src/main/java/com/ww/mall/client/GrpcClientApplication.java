package com.ww.mall.client;

import com.ww.mall.mongodb.annotation.EnableAppMongodb;
import com.ww.mall.rabbitmq.annotation.EnableAppRabbitmq;
import com.ww.mall.websocket.annotation.EnableAppWebSocket;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableAppMongodb
@EnableAppRabbitmq
@EnableAppWebSocket
@EnableDiscoveryClient
@SpringBootApplication
public class GrpcClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrpcClientApplication.class, args);
    }

}
