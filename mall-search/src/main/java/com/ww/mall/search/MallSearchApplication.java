package com.ww.mall.search;

import com.ww.mall.es.EnableMallElasticsearch;
import com.ww.mall.redis.EnableMallRedis;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableMallRedis
@EnableDiscoveryClient
@EnableMallElasticsearch
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class MallSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallSearchApplication.class, args);
    }

}
