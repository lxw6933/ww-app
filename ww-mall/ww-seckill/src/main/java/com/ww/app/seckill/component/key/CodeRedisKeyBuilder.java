package com.ww.app.seckill.component.key;

import com.ww.app.redis.key.RedisKeyBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-12-23- 15:47
 * @description:
 */
@Slf4j
@Component
public class CodeRedisKeyBuilder extends RedisKeyBuilder {

    private static final String CODES_LIST_KEY = "codes";
    private static final String OUT_ORDER_SET_KEY = "outOrderCode";

    public String buildCodesListKey(String actCode) {
        return super.getPrefix() + CODES_LIST_KEY + SPLIT_ITEM + actCode;
    }

    public String buildOutOrderSetKey() {
        return super.getPrefix() + OUT_ORDER_SET_KEY + SPLIT_ITEM;
    }

}
