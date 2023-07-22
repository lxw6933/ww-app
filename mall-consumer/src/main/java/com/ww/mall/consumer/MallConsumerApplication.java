package com.ww.mall.consumer;

import com.ww.mall.mongodb.EnableMallMongodb;
import com.ww.mall.redis.EnableMallRedis;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@RefreshScope
@EnableRabbit
@EnableMallRedis
@EnableMallMongodb
@EnableDiscoveryClient
@SpringBootApplication
public class MallConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MallConsumerApplication.class, args);
	}

}
