package com.ww.app.redis.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-12-29 14:59
 * @description:
 */
@Slf4j
@Component
public class DataRecordRedisComponent {

    private static final String BOUNDED_DATA_LIST_SCRIPT = "local key = KEYS[1]\n" +
            "local message = ARGV[1]\n" +
            "local max_length = tonumber(ARGV[2])\n" +
            "redis.call('LPUSH', key, message)\n" +
            "redis.call('LTRIM', key, 0, max_length - 1)";
    private String boundedDataListScriptSha1;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @PostConstruct
    public void init() {
        // 预加载的lua脚本
        preloadLuaScript();
    }

    /**
     * 预加载lua script，redis服务重启，需重新加载脚本，否则报错NOSCRIPT
     */
    private void preloadLuaScript() {
        boundedDataListScriptSha1 = stringRedisTemplate.execute((RedisCallback<String>) connection -> connection.scriptLoad(BOUNDED_DATA_LIST_SCRIPT.getBytes()));
    }

    /**
     * 插入数据到有界list中
     *
     * @param key key
     * @param data 数据
     * @param capacity 容量
     */
    public void pushDataToBoundedList(String key, String data, int capacity) {
        stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
            RedisSerializer keySerializer = stringRedisTemplate.getKeySerializer();
            byte[] keyBytes = keySerializer.serialize(key);
            // 执行lua脚本
            connection.evalSha(boundedDataListScriptSha1, ReturnType.INTEGER, 1, keyBytes, data.getBytes(), String.valueOf(capacity).getBytes());
            return null;
        });
    }

}
