package com.ww.app.redis.component.key;

import cn.hutool.core.util.StrUtil;
import com.ww.app.redis.key.RedisKeyBuilder;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-12-31- 17:11
 * @description:
 */
@Component
public class AppLockRedisKeyBuilder extends RedisKeyBuilder {

    private static final String LOCK_KEY = "lock";

    public String buildLock(String md5) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, LOCK_KEY, md5);
    }
}
