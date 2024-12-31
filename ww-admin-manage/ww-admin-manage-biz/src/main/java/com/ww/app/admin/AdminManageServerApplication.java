package com.ww.app.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class AdminManageServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminManageServerApplication.class, args);
    }

}
