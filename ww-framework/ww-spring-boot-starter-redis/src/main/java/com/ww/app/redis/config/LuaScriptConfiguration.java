package com.ww.app.redis.config;

import com.ww.app.redis.component.stock.constant.StockLuaConstant;
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
    public DefaultRedisScript<Long> rateLimitScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/rate_limit_script.lua")));
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    @Bean
    public DefaultRedisScript<Long> decrementStockScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
//        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/decrement_stock_script.lua")));
        redisScript.setScriptText(StockLuaConstant.DECREMENT_STOCK_LUA);
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    @Bean
    public DefaultRedisScript<Long> lockHashStockScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/lock_stock_hash_script.lua")));
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    @Bean
    public DefaultRedisScript<Long> useHashStockScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/use_stock_hash_script.lua")));
        redisScript.setResultType(Long.class);
        return redisScript;
    }

}
