package com.ww.mall.seckill;

import com.ww.mall.excel.annotation.EnableAppEasyExcel;
import com.ww.mall.ip.annotation.EnableAppIp2Region;
import com.ww.mall.minio.annotation.EnableAppMinio;
import com.ww.mall.mongodb.annotation.EnableAppMongodb;
import com.ww.mall.rabbitmq.annotation.EnableAppRabbitmq;
import com.ww.mall.redis.annotation.EnableAppRedis;
import com.ww.mall.redis.annotation.EnableAppRedisson;
import com.ww.mall.sensitive.annotation.EnableAppSensitiveWord;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableAppIp2Region
@EnableAppEasyExcel
@EnableAppSensitiveWord
@EnableAppMinio
//@EnableMallXxlJob
@EnableAppRedis
@EnableAppRedisson
@EnableAppMongodb
@EnableAppRabbitmq
@EnableDiscoveryClient
@SpringBootApplication
public class SeckillServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillServerApplication.class, args);
    }

}
