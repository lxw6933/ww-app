package com.ww.mall.open;

import com.ww.mall.mongodb.EnableMallMongodb;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallMongodb
@EnableDiscoveryClient
@SpringBootApplication
// 使用mybatis-plus-boot-starter依赖，如果mapper接口在springboot扫描包内，且都贴有@Mapper注解，就不要使用@MapperScan注解，避免重复扫描，使得项目启动变慢
//@MapperScan("com.ww.mall.open.domain.*.infrastructure")
public class MallOpenPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallOpenPlatformApplication.class, args);
    }

}
