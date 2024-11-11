package com.ww.mall.influxdb.annotation;

import com.ww.mall.influxdb.config.InfluxDBAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ww
 * @create 2024-07-27- 09:37
 * @description:
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({InfluxDBAutoConfiguration.class})
public @interface EnableMallInfluxDB {
}
