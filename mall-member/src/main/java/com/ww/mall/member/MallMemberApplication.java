package com.ww.mall.member;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author ww
 */
@RefreshScope
@EnableDiscoveryClient
@SpringBootApplication
@MapperScan("com.ww.mall.member.dao")
@EnableFeignClients(basePackages = "com.ww.mall.member.feign")
public class MallMemberApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallMemberApplication.class, args);
    }

}
