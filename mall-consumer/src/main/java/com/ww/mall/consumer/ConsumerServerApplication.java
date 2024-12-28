package com.ww.mall.consumer;

import com.ww.mall.mongodb.annotation.EnableAppMongodb;
import com.ww.mall.rabbitmq.annotation.EnableAppRabbitmq;
import com.ww.mall.redis.annotation.EnableAppRedis;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableAppRedis
@EnableAppMongodb
@EnableAppRabbitmq
@EnableDiscoveryClient
@SpringBootApplication
public class ConsumerServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConsumerServerApplication.class, args);
	}

}
