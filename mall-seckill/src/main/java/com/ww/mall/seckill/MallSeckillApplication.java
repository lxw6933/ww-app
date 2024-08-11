package com.ww.mall.seckill;

import com.ww.mall.annotation.enable.EnableMallEasyExcel;
import com.ww.mall.annotation.enable.EnableMallIp2Region;
import com.ww.mall.annotation.enable.EnableMallMinio;
import com.ww.mall.annotation.enable.EnableMallMongodb;
import com.ww.mall.annotation.enable.EnableMallRabbitmq;
import com.ww.mall.annotation.enable.EnableMallRedis;
import com.ww.mall.annotation.enable.EnableMallRedisson;
import com.ww.mall.annotation.enable.EnableMallSensitiveWord;
import com.ww.mall.annotation.enable.EnableMallXxlJob;
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
