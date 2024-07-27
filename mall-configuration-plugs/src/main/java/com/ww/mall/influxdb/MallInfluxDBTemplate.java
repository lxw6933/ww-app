package com.ww.mall.influxdb;

import cn.hutool.core.map.MapUtil;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.ww.mall.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author ww
 * @create 2024-07-27- 09:33
 * @description:
 */
@Slf4j
@Component
public class MallInfluxDBTemplate {

    private final InfluxDBClient influxDBClient;

    public MallInfluxDBTemplate(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
    }

    public Point buildPoint(String measurement, Map<String, String> tagMap, Map<String, Object> fieldValueMap) {
        Point point = Point.measurement(measurement).time(Instant.now(), WritePrecision.NS);
        if (MapUtil.isNotEmpty(tagMap)) {
            point = point.addTags(tagMap);
        }
        if (MapUtil.isNotEmpty(fieldValueMap)) {
            point = point.addFields(fieldValueMap);
        }
        return point;
    }

    public void writeData(String measurement, Map<String, String> tagMap, Map<String, Object> fieldValueMap) {
        Point point = this.buildPoint(measurement, tagMap, fieldValueMap);
        influxDBClient.getWriteApiBlocking().writePoint(point);
    }

    public void writeBatchData(List<Point> points) {
        influxDBClient.makeWriteApi().writePoints(points);
    }

    public <T> List<T> queryData(String fluxQuery, Class<T> clazz) {
        List<FluxTable> tables = influxDBClient.getQueryApi().query(fluxQuery);
        List<T> resultList = new ArrayList<>();
        try {
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    T obj = clazz.getDeclaredConstructor().newInstance();
                    for (Field field : clazz.getDeclaredFields()) {
                        field.setAccessible(true);
                        Object value = record.getValueByKey(field.getName());
                        if (value != null) {
                            field.set(obj, value);
                        }
                    }
                    resultList.add(obj);
                }
            }
        } catch (Exception e) {
            log.error("influxdb数据处理异常", e);
            throw new ApiException("数据处理异常");
        }
        return resultList;
    }

    public void close() {
        if (influxDBClient != null) {
            influxDBClient.close();
        }
    }

}
