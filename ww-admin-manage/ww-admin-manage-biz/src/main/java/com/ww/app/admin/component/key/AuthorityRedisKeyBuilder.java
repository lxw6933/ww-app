package com.ww.app.admin.component.key;

import com.ww.app.redis.key.RedisKeyBuilder;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-12-21 16:08
 * @description: 手机验证码 builder
 */
@Component
public class AuthorityRedisKeyBuilder extends RedisKeyBuilder {

    private static final String USER_AUTHORITIES_KEY = "user_authorities";

    public String buildUserAuthoritiesKey(Long userId) {
        return super.getPrefix() + USER_AUTHORITIES_KEY + SPLIT_ITEM + userId;
    }

}
