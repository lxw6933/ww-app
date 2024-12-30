package com.ww.app.client;

import com.ww.app.mongodb.annotation.EnableAppMongodb;
import com.ww.app.rabbitmq.annotation.EnableAppRabbitmq;
import com.ww.app.websocket.annotation.EnableAppWebSocket;
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
