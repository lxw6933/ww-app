package com.ww.mall.cart.component.key;

import com.ww.mall.redis.key.RedisKeyBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-12-23- 16:12
 * @description:
 */
@Slf4j
@Component
public class CartRedisKeyBuilder extends RedisKeyBuilder {

    public final static String CART_PREFIX = "cart";

    public String buildUserCartKey(Object userFlag) {
        return super.getPrefix() + CART_PREFIX + SPLIT_ITEM + userFlag;
    }

}
