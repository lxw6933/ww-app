package com.ww.app.flink;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class FlinkServerApplication {
    public static void main(String[] args) {
        System.out.println("Hello world!");
    }
}
