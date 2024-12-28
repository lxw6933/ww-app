package com.ww.mall.flink;

import com.ww.mall.minio.annotation.EnableAppMinio;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableAppMinio
@EnableDiscoveryClient
@SpringBootApplication
public class FlinkServerApplication {
    public static void main(String[] args) {
        System.out.println("Hello world!");
    }
}