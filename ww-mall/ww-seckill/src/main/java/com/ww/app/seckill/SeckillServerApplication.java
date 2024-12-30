package com.ww.app.seckill;

import com.ww.app.excel.annotation.EnableAppEasyExcel;
import com.ww.app.ip.annotation.EnableAppIp2Region;
import com.ww.app.minio.annotation.EnableAppMinio;
import com.ww.app.mongodb.annotation.EnableAppMongodb;
import com.ww.app.rabbitmq.annotation.EnableAppRabbitmq;
import com.ww.app.redis.annotation.EnableAppRedis;
import com.ww.app.redis.annotation.EnableAppRedisson;
import com.ww.app.sensitive.annotation.EnableAppSensitiveWord;
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
