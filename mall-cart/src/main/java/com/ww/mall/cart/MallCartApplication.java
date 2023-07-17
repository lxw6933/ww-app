package com.ww.mall.cart;

import com.ww.mall.redis.EnableMallRedis;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.openfeign.EnableFeignClients;

@RefreshScope
@EnableMallRedis
@EnableDiscoveryClient
@SpringBootApplication
@EnableFeignClients(basePackages = "com.ww.mall.cart.feign")
public class MallCartApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallCartApplication.class, args);
    }

}
