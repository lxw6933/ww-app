package com.ww.mall.influxdb.config;

import lombok.Data;
import lombok.Value;
import org.checkerframework.checker.units.qual.C;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2024-11-11- 16:27
 * @description:
 */
@Data
@Configuration
@ConfigurationProperties(prefix = InfluxDBProperties.PREFIX)
public class InfluxDBProperties {

    public static final String PREFIX = "influxdb";

    private String url;

    private String token;

    private String org;

    private String bucket;

}
