package com.ww.mall.flink;

import com.ww.mall.minio.annotation.EnableMallMinio;
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