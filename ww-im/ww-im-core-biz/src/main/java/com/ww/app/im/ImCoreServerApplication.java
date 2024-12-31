package com.ww.app.im;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class ImCoreServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImCoreServerApplication.class, args);
    }

}
