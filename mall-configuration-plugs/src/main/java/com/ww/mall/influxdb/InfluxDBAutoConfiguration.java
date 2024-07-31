package com.ww.mall.influxdb;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2024-07-27- 09:35
 * @description:
 */
@Configuration(proxyBeanMethods = false)
public class InfluxDBAutoConfiguration {

    @Value("${influxdb.url}")
    private String url;

    @Value("${influxdb.token}")
    private String token;

    @Value("${influxdb.org}")
    private String org;

    @Value("${influxdb.bucket}")
    private String bucket;

    @Bean
    public InfluxDBClient influxDBClient() {
        return InfluxDBClientFactory.create(url, token.toCharArray(), org, bucket);
    }

    @Bean
    public MallInfluxDBTemplate mallInfluxDBTemplate(InfluxDBClient influxDBClient) {
        return new MallInfluxDBTemplate(influxDBClient);
    }

}
