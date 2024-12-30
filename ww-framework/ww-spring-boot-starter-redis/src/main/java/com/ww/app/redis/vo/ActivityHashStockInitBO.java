package com.ww.app.redis.vo;

import lombok.Data;

import java.util.Map;

/**
 * @author ww
 * @create 2024-06-26- 15:42
 * @description:
 */
@Data
public class ActivityHashStockInitBO {

    /**
     * hashKey
     */
    private String stockHashKey;

    /**
     * key: field
     * value: stock
     */
    private Map<String, String> dataMap;

}
