package com.ww.app.im;

import com.ww.app.redis.annotation.EnableAppRedis;
import com.ww.app.redis.annotation.EnableAppRedisson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableAppRedis
@EnableAppRedisson
@EnableDiscoveryClient
@SpringBootApplication
public class ImCoreServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImCoreServerApplication.class, args);
    }

}
