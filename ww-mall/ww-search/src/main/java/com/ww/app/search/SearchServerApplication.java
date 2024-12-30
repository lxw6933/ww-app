package com.ww.app.search;

import com.ww.app.es.annotation.EnableAppElasticsearch;
import com.ww.app.mongodb.annotation.EnableAppMongodb;
import com.ww.app.redis.annotation.EnableAppRedis;
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
