package com.ww.mall.member;

import com.ww.mall.rabbitmq.EnableMallRabbitmq;
import com.ww.mall.redis.EnableMallRedis;
import com.ww.mall.redis.EnableMallRedisson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @author ww
 */
@EnableMallRedis
@EnableMallRedisson
@EnableMallRabbitmq
@EnableDiscoveryClient
@SpringBootApplication
public class MallMemberApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallMemberApplication.class, args);
    }

}
