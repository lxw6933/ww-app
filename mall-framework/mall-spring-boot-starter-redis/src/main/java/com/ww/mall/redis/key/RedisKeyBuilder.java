package com.ww.mall.redis.key;

import org.springframework.beans.factory.annotation.Value;

/**
 * @author ww
 * @create 2024-12-21 10:34
 * @description:
 */
public class RedisKeyBuilder {

    @Value("${spring.application.name}")
    private String applicationName;

    public static final String SPLIT_ITEM = ":";

    public String getPrefix() {
        return applicationName + SPLIT_ITEM;
    }

}
