package com.ww.mall.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class MallRedisUtil {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @SuppressWarnings("all")
    public Long decrementStock(String key, long decrement) {
        return redisTemplate.execute((RedisCallback<Long>) connection -> {
            RedisSerializer keySerializer = redisTemplate.getKeySerializer();
            byte[] keyBytes = keySerializer.serialize(key);

            String script = "local current_stock = tonumber(redis.call('get', KEYS[1]) or 0);\n" +
                    "if current_stock >= tonumber(ARGV[1]) then\n" +
                    "    redis.call('decrby', KEYS[1], tonumber(ARGV[1]));\n" +
                    "    return current_stock - tonumber(ARGV[1]);\n" +
                    "else\n" +
                    "    return -1;\n" +
                    "end";

            Object result = connection.eval(script.getBytes(), ReturnType.INTEGER, 1, keyBytes,
                    String.valueOf(decrement).getBytes());
            return (Long) result;
        });
    }

    public Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        redisTemplate.execute(connect -> {
            Set<String> binaryKeys = new HashSet<>();
            Cursor<byte[]> cursor = connect.scan(
                    ScanOptions.scanOptions().match(pattern).count(100000).build()
            );
            while (cursor.hasNext() && binaryKeys.size() < 100000) {
                binaryKeys.add(new String(cursor.next()));
            }
            keys.addAll(binaryKeys);
            return binaryKeys;
        }, true);
        return keys;
    }

    public void batchRemoveKeys(Set<String> keys) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            keys.forEach(key -> connection.del(key.getBytes()));
            return null;
        });
    }

    public void batchInitializeData(Map<String, String> dataMap) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            dataMap.forEach((key, value) -> connection.set(key.getBytes(), value.getBytes()));
            return null;
        });
    }

}
