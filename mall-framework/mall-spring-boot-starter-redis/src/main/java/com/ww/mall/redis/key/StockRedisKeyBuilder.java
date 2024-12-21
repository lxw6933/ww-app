package com.ww.mall.redis.key;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ww
 * @create 2024-12-21 10:38
 * @description: 库存key builder
 */
@Component
public class StockRedisKeyBuilder extends RedisKeyBuilder {

    private static final String STOCK_KEY = "stock";

    /**
     * 生成redis stock key
     *
     * @param activityCode 活动编码
     * @param subActivityCode 场次编码
     * @param spuCode 商品编码
     * @param skuId skuId
     * @return stockKey
     */
    public String buildStockKey(String activityCode, String subActivityCode, String spuCode, Long skuId) {
        List<Object> keys = new ArrayList<>();
        keys.add(STOCK_KEY);
        if (StringUtils.isNotEmpty(activityCode)) {
            keys.add(activityCode);
        }
        if (StringUtils.isNotEmpty(subActivityCode)) {
            keys.add(subActivityCode);
        }
        keys.add(spuCode);
        keys.add(skuId);
        return super.getPrefix() + StringUtils.joinWith(SPLIT_ITEM, keys.toArray());
    }

}
