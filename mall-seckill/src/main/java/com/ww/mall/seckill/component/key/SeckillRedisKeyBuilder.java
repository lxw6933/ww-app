package com.ww.mall.seckill.component.key;

import cn.hutool.core.util.StrUtil;
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

    private static final String RED_PACKET_KEY = "redPacket";

    public String buildSeckillPathKey(String activityCode, Long userId, Long skuId) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, SECKILL_PATH_KEY, activityCode, userId, skuId);
    }

    public String buildSeckillCodeKey(String activityCode, Long userId, Long skuId) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, SECKILL_CODE_KEY, activityCode, userId, skuId);
    }

    public String buildRedPacketKey(String redPacketCode) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, RED_PACKET_KEY, redPacketCode);
    }

}
