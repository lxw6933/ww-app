package com.ww.mall.member;

import com.ww.mall.annotation.enable.EnableMallRabbitmq;
import com.ww.mall.annotation.enable.EnableMallRedis;
import com.ww.mall.annotation.enable.EnableMallRedisson;
import com.ww.mall.mybatis.annotation.EnableMallMybatis;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallRedis
@EnableMallRedisson
@EnableMallRabbitmq
@EnableMallMybatis
@EnableDiscoveryClient
@SpringBootApplication
public class MallMemberBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallMemberBizApplication.class, args);
    }

}
