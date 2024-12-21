package com.ww.mall.seckill.component.key;

import com.ww.mall.redis.key.RedisKeyBuilder;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-12-21 16:08
 * @description: 秒杀key builder
 */
@Component
public class SeckillRedisKeyBuilder extends RedisKeyBuilder {

    private static final String SECKILL_PATH_KEY = "seckill_path";
    private static final String SECKILL_CODE_KEY = "seckill_code";

    public String buildSeckillPathKey(String activityCode, Long userId, Long skuId) {
        return super.getPrefix() + SECKILL_PATH_KEY + SPLIT_ITEM + activityCode + SPLIT_ITEM + userId + SPLIT_ITEM + skuId;
    }

    public String buildSeckillCodeKey(String activityCode, Long userId, Long skuId) {
        return super.getPrefix() + SECKILL_CODE_KEY + SPLIT_ITEM + activityCode + SPLIT_ITEM + userId + SPLIT_ITEM + skuId;
    }

}
