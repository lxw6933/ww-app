package com.ww.mall.promotion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class PromotionServerBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromotionServerBizApplication.class, args);
    }

}
