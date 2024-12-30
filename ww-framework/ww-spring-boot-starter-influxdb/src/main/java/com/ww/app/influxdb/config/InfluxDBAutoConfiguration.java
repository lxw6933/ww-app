package com.ww.app.influxdb.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.ww.app.influxdb.InfluxDBTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2024-07-27- 09:35
 * @description:
 */
@Configuration(proxyBeanMethods = false)
public class InfluxDBAutoConfiguration {

    @Bean
    public InfluxDBProperties influxDBProperties() {
        return new InfluxDBProperties();
    }

    @Bean
    public InfluxDBClient influxDBClient(InfluxDBProperties influxDBProperties) {
        return InfluxDBClientFactory.create(influxDBProperties.getUrl(), influxDBProperties.getToken().toCharArray(), influxDBProperties.getOrg(), influxDBProperties.getBucket());
    }

    @Bean
    public InfluxDBTemplate mallInfluxDBTemplate(InfluxDBClient influxDBClient) {
        return new InfluxDBTemplate(influxDBClient);
    }

}
