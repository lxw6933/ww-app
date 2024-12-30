package com.ww.app.influxdb;

import cn.hutool.core.map.MapUtil;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.ww.app.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;

/**
 * @author ww
 * @create 2024-07-27- 09:33
 * @description:
 */
@Slf4j
@Component
public class InfluxDBTemplate {

    private final InfluxDBClient influxDBClient;

    public InfluxDBTemplate(InfluxDBClient influxDBClient) {
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

    public <T> void writeObjectData(T data) {
        influxDBClient.getWriteApiBlocking().writeMeasurement(WritePrecision.NS, data);
    }

    public <T> void writeBatchObjectData(List<T> dataList) {
        influxDBClient.getWriteApiBlocking().writeMeasurements(WritePrecision.NS, dataList);
    }

    public void writeData(String measurement, Map<String, String> tagMap, Map<String, Object> fieldValueMap) {
        Point point = this.buildPoint(measurement, tagMap, fieldValueMap);
        influxDBClient.getWriteApiBlocking().writePoint(point);
    }

    public void writeBatchData(List<Point> points) {
        influxDBClient.makeWriteApi().writePoints(points);
    }

    public <T> List<T> queryData(String fluxQuery, Class<T> clazz, List<String> tageFieldNameList) {
        List<FluxTable> tables = influxDBClient.getQueryApi().query(fluxQuery);
        List<T> resultList = new ArrayList<>();
        // 字段map【tables.size() 表示T有多少个字段】
        Map<String, Map<String, Object>> objMap = new HashMap<>();
        try {
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    // tag handler
                    Map<String, String> tagMap = new HashMap<>();
                    for (String tageFieldName : tageFieldNameList) {
                        String tagFieldValue = Objects.requireNonNull(record.getValueByKey(tageFieldName)).toString();
                        tagMap.put(tageFieldName, tagFieldValue);
                    }
                    StringBuilder uniqueKey = new StringBuilder();
                    if (!tagMap.isEmpty()) {
                        for (String tagValue : tagMap.values()) {
                            uniqueKey.append(tagValue);
                        }
                    }
                    // tag field value set
                    String time = Objects.requireNonNull(record.getTime()).toString();
                    Map<String, Object> fieldMap = objMap.getOrDefault(uniqueKey + time, new HashMap<>());
                    if (!tagMap.isEmpty()) {
                        fieldMap.putAll(tagMap);
                    }
                    String fieldName = Objects.requireNonNull(record.getField());
                    Object fieldValue = record.getValue();
                    fieldMap.put(fieldName, fieldValue);
                    objMap.putIfAbsent(uniqueKey + time, fieldMap);
                }
            }
            objMap.forEach((time, fieldMap) -> {
                try {
                    T entity = clazz.getDeclaredConstructor().newInstance();
                    fieldMap.forEach((fieldName, fieldValue) -> {
                        try {
                            Field field = clazz.getDeclaredField(fieldName);
                            field.setAccessible(true);
                            if (field.getType().isAssignableFrom(fieldValue.getClass())) {
                                field.set(entity, fieldValue);
                            } else if (field.getType() == String.class) {
                                field.set(entity, fieldValue.toString());
                            } else if (field.getType() == Double.class) {
                                field.set(entity, Double.parseDouble(fieldValue.toString()));
                            } else if (field.getType() == Integer.class) {
                                field.set(entity, Integer.parseInt(fieldValue.toString()));
                            } else if (field.getType() == Date.class) {
                                field.set(entity, new Date(Long.parseLong(fieldValue.toString())));
                            }
                        } catch (Exception e) {
                            log.error("字段【{}】值【{}】设置异常", fieldName, fieldValue, e);
                        }
                    });
                    resultList.add(entity);
                } catch (Exception e) {
                    log.error("field数据处理异常", e);
                    throw new RuntimeException("field数据处理异常");
                }
            });
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
