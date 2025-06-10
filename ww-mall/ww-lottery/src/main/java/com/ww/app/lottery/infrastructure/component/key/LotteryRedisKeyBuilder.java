package com.ww.app.lottery.infrastructure.component.key;

import cn.hutool.core.util.StrUtil;
import com.ww.app.redis.key.RedisKeyBuilder;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2025-06-10- 13:46
 * @description:
 */
@Component
public class LotteryRedisKeyBuilder extends RedisKeyBuilder {

    private static final String LOTTERY_STOCK_KEY = "lottery_stock";

    public String buildLotteryStockPrefixKey(String prizeId) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, LOTTERY_STOCK_KEY, prizeId);
    }

}
