package com.ww.mall.influxdb;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    public void writeData(String measurement, String field, double value, String tagKey, String tagValue) {
        Point point = Point.measurement(measurement)
                .addTag(tagKey, tagValue)
                .addField(field, value)
                .time(Instant.now(), WritePrecision.NS);
        influxDBClient.getWriteApiBlocking().writePoint(point);
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
