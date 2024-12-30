package com.ww.app.cart;

import com.ww.app.redis.annotation.EnableAppRedis;
import com.ww.app.redis.annotation.EnableAppRedisson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableAppRedis
@EnableAppRedisson
@EnableDiscoveryClient
@SpringBootApplication
public class CartServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CartServerApplication.class, args);
    }

}
