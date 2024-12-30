package com.ww.app.auth.component.key;

import com.ww.app.redis.key.RedisKeyBuilder;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-12-21 16:08
 * @description: 手机验证码 builder
 */
@Component
public class SmsCodeRedisKeyBuilder extends RedisKeyBuilder {

    private static final String SMS_CODE_KEY = "sms_code";

    public String buildSmsCodeKey(String mobile) {
        return super.getPrefix() + SMS_CODE_KEY + SPLIT_ITEM + mobile;
    }

}
