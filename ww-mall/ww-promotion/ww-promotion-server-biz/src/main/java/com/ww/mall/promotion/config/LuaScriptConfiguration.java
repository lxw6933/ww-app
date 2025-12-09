package com.ww.mall.promotion.config;

import com.ww.app.redis.component.lua.RedisScriptComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import javax.annotation.PostConstruct;

/**
 * @author ww
 * @create 2025-11-10 14:43
 * @description:
 */
@Slf4j
@Configuration
public class LuaScriptConfiguration {

    public static final String CREATE_GROUP_SCRIPT_NAME = "create_group_script";
    public static final String JOIN_GROUP_SCRIPT_NAME = "join_group_script";
    public static final String EXPIRE_MARK_FAILED_SCRIPT_NAME = "expire_mark_failed_script";

    @Bean
    public DefaultRedisScript<Long> createGroupScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/group/create_group.lua")));
        redisScript.setResultType(Long.class);
        log.info("加载发起拼团lua脚本");
        return redisScript;
    }

    @Bean
    public DefaultRedisScript<Long> joinGroupScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/group/join_group.lua")));
        redisScript.setResultType(Long.class);
        log.info("加载加入拼团lua脚本");
        return redisScript;
    }

    @Bean
    public DefaultRedisScript<Long> expireMarkFailedScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/group/expire_mark_failed.lua")));
        redisScript.setResultType(Long.class);
        log.info("加载拼团过期标记失败lua脚本");
        return redisScript;
    }

    @PostConstruct
    public void init(RedisScriptComponent redisScriptComponent,
                     DefaultRedisScript<Long> createGroupScript,
                     DefaultRedisScript<Long> joinGroupScript,
                     DefaultRedisScript<Long> expireMarkFailedScript) {
        // 加载脚本到Redis服务器
        redisScriptComponent.preLoadLuaScript(CREATE_GROUP_SCRIPT_NAME, createGroupScript.getScriptAsString());
        redisScriptComponent.preLoadLuaScript(JOIN_GROUP_SCRIPT_NAME, joinGroupScript.getScriptAsString());
        redisScriptComponent.preLoadLuaScript(EXPIRE_MARK_FAILED_SCRIPT_NAME, expireMarkFailedScript.getScriptAsString());
    }

}
