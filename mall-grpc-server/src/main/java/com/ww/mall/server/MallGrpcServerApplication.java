package com.ww.mall.server;

import com.ww.mall.sensitive.EnableMallSensitiveWord;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallSensitiveWord
@EnableDiscoveryClient
@SpringBootApplication
public class MallGrpcServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallGrpcServerApplication.class, args);
    }

}
