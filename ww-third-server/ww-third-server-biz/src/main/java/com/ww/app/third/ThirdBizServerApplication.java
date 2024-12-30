package com.ww.app.third;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class ThirdBizServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThirdBizServerApplication.class, args);
    }

}
