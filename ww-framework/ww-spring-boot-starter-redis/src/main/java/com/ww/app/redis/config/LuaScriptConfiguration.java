package com.ww.app.redis.config;

import com.ww.app.redis.constant.LuaConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * @author ww
 * @create 2024-06-14- 13:46
 * @description:
 */
@Slf4j
@Configuration
public class LuaScriptConfiguration {

    @Bean
    public DefaultRedisScript<Long> decrementStockScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
//        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/decrement_stock_script.lua")));
        redisScript.setScriptText(LuaConstant.DECREMENT_STOCK_LUA);
        redisScript.setResultType(Long.class);
        log.info("load decrement stock redis lua script success");
        return redisScript;
    }

    @Bean
    public DefaultRedisScript<Long> lockHashStockScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/lock_stock_hash_script.lua")));
        redisScript.setResultType(Long.class);
        log.info("load lock stock redis hash lua script success");
        return redisScript;
    }

    @Bean
    public DefaultRedisScript<Long> useHashStockScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/use_stock_hash_script.lua")));
        redisScript.setResultType(Long.class);
        log.info("load use stock redis hash lua script success");
        return redisScript;
    }

}
