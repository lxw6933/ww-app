package com.ww.app.redis.component.key;

import com.ww.app.redis.key.RedisKeyBuilder;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-12-21 10:38
 * @description: 库存key builder
 */
@Component
public class GeoRedisKeyBuilder extends RedisKeyBuilder {

    private static final String GEO_KEY = "geo";

    public String buildGeoKey(Long typeId) {
        return super.getPrefix() + GEO_KEY + SPLIT_ITEM + typeId;
    }

}
