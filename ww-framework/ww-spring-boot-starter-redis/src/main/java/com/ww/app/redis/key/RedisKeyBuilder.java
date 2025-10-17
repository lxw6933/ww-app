package com.ww.app.redis.key;

import org.springframework.beans.factory.annotation.Value;

/**
 * @author ww
 * @create 2024-12-21 10:34
 * @description:
 */
public class RedisKeyBuilder {

    @Value("${spring.application.name}")
    private String applicationName;

    private static final String REDIS_SCRIPT_SHA1_KEY = "script_sha1";

    public static final String SPLIT_ITEM = ":";

    public static final String START = "*";

    public String getPrefix() {
        return applicationName + SPLIT_ITEM;
    }

    public String buildLuaScriptSha1Key(String scriptName) {
        return getPrefix() + REDIS_SCRIPT_SHA1_KEY + SPLIT_ITEM + scriptName;
    }

}
