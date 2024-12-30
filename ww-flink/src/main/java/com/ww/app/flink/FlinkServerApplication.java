package com.ww.app.flink;

import com.ww.app.minio.annotation.EnableAppMinio;
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
