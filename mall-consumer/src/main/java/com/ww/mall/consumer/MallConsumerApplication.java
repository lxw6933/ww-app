package com.ww.mall.consumer;

import com.ww.mall.annotation.enable.EnableMallMongodb;
import com.ww.mall.annotation.enable.EnableMallRabbitmq;
import com.ww.mall.redis.annotation.EnableMallRedis;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallRedis
@EnableMallMongodb
@EnableMallRabbitmq
@EnableDiscoveryClient
@SpringBootApplication
public class MallConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MallConsumerApplication.class, args);
	}

}
