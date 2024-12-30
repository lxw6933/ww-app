package com.ww.app.redis.key;

import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-12-21 16:30
 * @description: 商品 key builder
 */
@Component
public class SpuRedisKeyBuilder extends RedisKeyBuilder {

    public static final String SPU_SALE_KEY = "spu_sales";
    public static final String SPU_CREDIT_SCORE_KEY = "spu_score";

    public String buildSpuMapKey(Long channelId, Long spuId) {
        return channelId + SPLIT_ITEM + spuId;
    }

    public String buildSpuSaleKey(String spuMapKey) {
        return super.getPrefix() + SPU_SALE_KEY + SPLIT_ITEM + spuMapKey;
    }

    public String buildChannelSpuCreditScoreHashKey(Long channelId) {
        return super.getPrefix() + SPU_CREDIT_SCORE_KEY + SPLIT_ITEM + channelId;
    }

}
