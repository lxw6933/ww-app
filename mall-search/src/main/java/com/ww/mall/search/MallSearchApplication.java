package com.ww.mall.search;

import com.ww.mall.annotation.enable.EnableMallMongodb;
import com.ww.mall.es.annotation.EnableMallElasticsearch;
import com.ww.mall.redis.annotation.EnableMallRedis;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallRedis
@EnableMallMongodb
@EnableDiscoveryClient
@EnableMallElasticsearch
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class MallSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallSearchApplication.class, args);
    }

}
