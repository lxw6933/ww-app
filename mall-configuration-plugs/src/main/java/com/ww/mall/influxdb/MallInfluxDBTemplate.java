package com.ww.mall.influxdb;

import cn.hutool.core.map.MapUtil;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author ww
 * @create 2024-07-27- 09:33
 * @description:
 */
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

    public List<FluxRecord> queryData(String fluxQuery) {
        List<FluxTable> tables = influxDBClient.getQueryApi().query(fluxQuery);
        List<FluxRecord> records = new ArrayList<>();
        for (FluxTable table : tables) {
            records.addAll(table.getRecords());
        }
        return records;
    }

    public void close() {
        if (influxDBClient != null) {
            influxDBClient.close();
        }
    }

}
