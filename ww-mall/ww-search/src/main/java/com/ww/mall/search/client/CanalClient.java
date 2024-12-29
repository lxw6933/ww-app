package com.ww.mall.search.client;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author ww
 * @create 2023-07-25- 20:11
 * @description:
 */
@Slf4j
public class CanalClient {

    public static void main(String[] args) throws InterruptedException, InvalidProtocolBufferException {
        // 创建canal连接器
        CanalConnector canalConnector = CanalConnectors.newSingleConnector(new InetSocketAddress("127.0.0.1", 11111), "example", "", "");
        while (true) {
            // 获取连接
            canalConnector.connect();
            // 订阅监听数据库binlog
            canalConnector.subscribe("test.*");
            // 抓取100变更数据
            Message message = canalConnector.get(100);
            // 获取变更数据集合
            List<CanalEntry.Entry> entries = message.getEntries();
            // 如果不存在数据变更, 则等待一会继续拉取数据
            if (entries.isEmpty()) {
                Thread.sleep(1000);
            } else {
                // 遍历所有抓取到的变更数据，单条解析
                for (CanalEntry.Entry entry : entries) {
                    // 1.获取表名
                    String tableName = entry.getHeader().getTableName();
                    // 2.获取类型
                    CanalEntry.EntryType entryType = entry.getEntryType();
                    // 3.获取序列化后的数据
                    ByteString storeValue = entry.getStoreValue();
                    // 4.判断当前entryType类型是否为ROWDATA
                    if (CanalEntry.EntryType.ROWDATA.equals(entryType)) {
                        // 5.反序列化数据
                        CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(storeValue);
                        // 6.获取当前事件的操作类型
                        CanalEntry.EventType eventType = rowChange.getEventType();
                        // 7.获取数据集
                        List<CanalEntry.RowData> rowDataList = rowChange.getRowDatasList();
                        // 8.遍历rowDataList，并打印数据集
                        for (CanalEntry.RowData rowData : rowDataList) {
                            JSONObject beforeData = new JSONObject();
                            List<CanalEntry.Column> beforeColumnsList = rowData.getBeforeColumnsList();
                            for (CanalEntry.Column column : beforeColumnsList) {
                                beforeData.put(column.getName(), column.getValue());
                            }
                            JSONObject afterData = new JSONObject();
                            List<CanalEntry.Column> afterColumnsList = rowData.getAfterColumnsList();
                            for (CanalEntry.Column column : afterColumnsList) {
                                afterData.put(column.getName(), column.getValue());
                            }
                            // 数据打印
                            log.info("变更表:" + tableName +
                                    ",变更类型:" + eventType +
                                    "\n变更前数据:" + beforeData +
                                    "\n变更后数据:" + afterData);
                        }
                    } else {
                        log.info("当前操作类型为：" + entryType);
                    }
                }
            }
        }
    }

}
