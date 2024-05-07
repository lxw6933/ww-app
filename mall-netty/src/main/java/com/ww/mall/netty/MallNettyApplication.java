package com.ww.mall.netty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class MallNettyApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallNettyApplication.class, args);
    }

}
