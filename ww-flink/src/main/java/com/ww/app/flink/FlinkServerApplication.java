package com.ww.app.flink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Flink服务应用
 */
@EnableScheduling
@EnableDiscoveryClient
@SpringBootApplication
public class FlinkServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlinkServerApplication.class, args);
    }
}
