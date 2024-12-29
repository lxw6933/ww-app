package com.ww.mall.search;

import com.ww.mall.es.annotation.EnableAppElasticsearch;
import com.ww.mall.mongodb.annotation.EnableAppMongodb;
import com.ww.mall.redis.annotation.EnableAppRedis;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableAppRedis
@EnableAppMongodb
@EnableDiscoveryClient
@EnableAppElasticsearch
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class SearchServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchServerApplication.class, args);
    }

}
