package com.ww.mall.consumer;

import com.ww.mall.mongodb.EnableMallMongodb;
import com.ww.mall.rabbitmq.EnableMallRabbitmq;
import com.ww.mall.redis.EnableMallRedis;
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
