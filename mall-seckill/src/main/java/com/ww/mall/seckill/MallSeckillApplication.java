package com.ww.mall.seckill;

import com.ww.mall.annotation.enable.*;
import com.ww.mall.excel.annotation.EnableMallEasyExcel;
import com.ww.mall.redis.service.CodeGeneratorService;
import com.ww.mall.redis.service.IssueCodeService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

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
@Import({IssueCodeService.class, CodeGeneratorService.class})
public class MallSeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallSeckillApplication.class, args);
    }

}
