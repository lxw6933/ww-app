package com.ww.app.redpacket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class ImRedpacketServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImRedpacketServerApplication.class, args);
    }

}
