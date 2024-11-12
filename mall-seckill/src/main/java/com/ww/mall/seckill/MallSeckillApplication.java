package com.ww.mall.seckill;

import com.ww.mall.excel.annotation.EnableMallEasyExcel;
import com.ww.mall.ip.annotation.EnableMallIp2Region;
import com.ww.mall.minio.annotation.EnableMallMinio;
import com.ww.mall.mongodb.annotation.EnableMallMongodb;
import com.ww.mall.rabbitmq.annotation.EnableMallRabbitmq;
import com.ww.mall.redis.annotation.EnableMallRedis;
import com.ww.mall.redis.annotation.EnableMallRedisson;
import com.ww.mall.sensitive.annotation.EnableMallSensitiveWord;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallIp2Region
@EnableMallEasyExcel
@EnableMallSensitiveWord
@EnableMallMinio
//@EnableMallXxlJob
@EnableMallRedis
@EnableMallRedisson
@EnableMallMongodb
@EnableMallRabbitmq
@EnableDiscoveryClient
@SpringBootApplication
public class MallSeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallSeckillApplication.class, args);
    }

}
