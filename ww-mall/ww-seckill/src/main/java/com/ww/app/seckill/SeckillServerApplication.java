package com.ww.app.seckill;

import com.ww.app.redis.component.pvuv.EnableAppRedisPuUvComponent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
@EnableAppRedisPuUvComponent
public class SeckillServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillServerApplication.class, args);
    }

}
