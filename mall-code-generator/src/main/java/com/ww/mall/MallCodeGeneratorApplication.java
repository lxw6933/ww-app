package com.ww.mall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

@EnableDiscoveryClient
@SpringBootApplication
@ComponentScans({
        @ComponentScan("com.ww.mall.common")
})
public class MallCodeGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallCodeGeneratorApplication.class, args);
    }

}
