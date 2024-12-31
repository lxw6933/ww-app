package com.ww.app.redpacket;

import com.ww.app.mongodb.annotation.EnableAppMongodb;
import com.ww.app.redis.annotation.EnableAppRedis;
import com.ww.app.redis.annotation.EnableAppRedisson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableAppRedis
@EnableAppMongodb
@EnableAppRedisson
@EnableDiscoveryClient
@SpringBootApplication
public class ImRedpacketServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImRedpacketServerApplication.class, args);
    }

}
