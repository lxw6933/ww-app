package com.ww.mall.flink;

import com.ww.mall.annotation.enable.EnableMallMinio;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallMinio
@EnableDiscoveryClient
@SpringBootApplication
public class MallFlinkApplication {
    public static void main(String[] args) {
        System.out.println("Hello world!");
    }
}