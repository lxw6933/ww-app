package com.ww.mall.client;

import com.ww.mall.netty.annotation.EnableMallWebSocket;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallWebSocket
@EnableDiscoveryClient
@SpringBootApplication
public class MallGrpcClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallGrpcClientApplication.class, args);
    }

}
