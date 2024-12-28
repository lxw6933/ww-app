package com.ww.mall.consumer.server.canal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @author ww
 * @create 2023-07-27- 10:03
 * @description: 阿里Canal中间件消息接收实体类
 */
@Data
@NoArgsConstructor
public class CanalMessage<T> {

    /**
     * 事件ID,没有实际意义
     */
    @JsonProperty("id")
    private Long id;

    /**
     * 当前更变后节点数据
     */
    @JsonProperty("data")
    private List<T> data;

    /**
     * 当前更变前节点数据
     */
    @JsonProperty("old")
    private List<T> old;

    /**
     * 数据库名称
     */
    @JsonProperty("database")
    private String database;

    /**
     * 表名称
     */
    @JsonProperty("table")
    private String table;

    /**
     * binlog execute time
     */
    @JsonProperty("es")
    private Long es;

    /**
     * 是否DDL语句
     */
    @JsonProperty("isDdl")
    private Boolean isDdl;

    /**
     * SQL类型映射
     */
    @JsonProperty("sqlType")
    private Map<String, Integer> sqlType;

    /**
     * MySQL字段类型映射
     */
    @JsonProperty("mysqlType")
    private Map<String, String> mysqlType;

    /**
     * 主键列名称列表
     */
    @JsonProperty("pkNames")
    private List<String> pkNames;

    /**
     * 执行的sql,不一定存在
     */
    @JsonProperty("sql")
    private String sql;

    /**
     * dml build timestamp
     */
    @JsonProperty("ts")
    private Long ts;

    /**
     * 类型 UPDATE\INSERT\DELETE\QUERY
     */
    @JsonProperty("type")
    private String type;
}



