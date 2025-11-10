package com.ww.app.member.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * @author ww
 * @create 2025-11-10 14:43
 * @description:
 */
@Slf4j
@Configuration
public class LuaScriptConfiguration {

    @Bean
    public DefaultRedisScript<Object> signScript() {
        DefaultRedisScript<Object> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/sign.lua")));
        redisScript.setResultType(Object.class);
        log.info("加载签到lua脚本");
        return redisScript;
    }

}
