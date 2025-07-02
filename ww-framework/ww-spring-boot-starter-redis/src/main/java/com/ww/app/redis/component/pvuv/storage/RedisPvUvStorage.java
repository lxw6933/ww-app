package com.ww.app.redis.component.pvuv.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis PV/UV存储实现
 * 默认使用批处理模式和HyperLogLog算法
 */
@Slf4j
public class RedisPvUvStorage {

    /**
     * Redis模板
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 构造函数
     *
     * @param stringRedisTemplate Redis模板
     */
    public RedisPvUvStorage(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 增加PV计数
     *
     * @param key 键
     * @return 增加后的值
     */
    public long incrementPv(String key) {
        Long result = stringRedisTemplate.opsForValue().increment(key);
        return result != null ? result : 0;
    }

    /**
     * 增加指定值的PV计数
     *
     * @param key 键
     * @param increment 增加的值
     * @return 增加后的值
     */
    public long incrementPv(String key, long increment) {
        if (increment <= 0) {
            return getPv(key);
        }
        
        Long result = stringRedisTemplate.opsForValue().increment(key, increment);
        return result != null ? result : 0;
    }

    /**
     * 批量增加PV计数
     *
     * @param pvData 键值对
     */
    public void batchIncrementPv(Map<String, Long> pvData) {
        if (pvData.isEmpty()) {
            return;
        }

        final int batchSize = 500; // 单次批处理大小
        
        // 当数据量大时使用分批处理
        if (pvData.size() > batchSize) {
            List<Map.Entry<String, Long>> entries = new ArrayList<>(pvData.entrySet());
            for (int i = 0; i < entries.size(); i += batchSize) {
                int end = Math.min(i + batchSize, entries.size());
                List<Map.Entry<String, Long>> batch = entries.subList(i, end);
                
                // 使用管道批量增加PV计数
                stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    for (Map.Entry<String, Long> entry : batch) {
                        byte[] keyBytes = entry.getKey().getBytes(StandardCharsets.UTF_8);
                        connection.incrBy(keyBytes, entry.getValue());
                    }
                    return null;
                });
            }
        } else {
            // 使用管道批量增加PV计数
            stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (Map.Entry<String, Long> entry : pvData.entrySet()) {
                    byte[] keyBytes = entry.getKey().getBytes(StandardCharsets.UTF_8);
                    connection.incrBy(keyBytes, entry.getValue());
                }
                return null;
            });
        }
    }

    /**
     * 添加用户到UV集合（使用HyperLogLog）
     *
     * @param key 键
     * @param userId 用户标识
     * @return 如果是新增用户返回true
     */
    public boolean addUserToUv(String key, String userId) {
        if (userId == null || userId.isEmpty()) {
            return false;
        }

        Long pfAdd = stringRedisTemplate.opsForHyperLogLog().add(key, userId);
        return pfAdd != null && pfAdd > 0;
    }

    /**
     * 批量添加用户到UV集合（使用HyperLogLog）
     *
     * @param uvData 键值对
     */
    public void batchAddUsersToUv(Map<String, Set<String>> uvData) {
        if (uvData.isEmpty()) {
            return;
        }

        final int batchSize = 500; // 单次批处理大小

        // HyperLogLog批量处理
        if (uvData.size() > batchSize) {
            List<Map.Entry<String, Set<String>>> entries = new ArrayList<>(uvData.entrySet());
            for (int i = 0; i < entries.size(); i += batchSize) {
                int end = Math.min(i + batchSize, entries.size());
                List<Map.Entry<String, Set<String>>> batch = entries.subList(i, end);
                
                for (Map.Entry<String, Set<String>> entry : batch) {
                    String key = entry.getKey();
                    Set<String> users = entry.getValue();
                    if (!users.isEmpty()) {
                        stringRedisTemplate.opsForHyperLogLog().add(key, users.toArray(new String[0]));
                    }
                }
            }
        } else {
            for (Map.Entry<String, Set<String>> entry : uvData.entrySet()) {
                String key = entry.getKey();
                Set<String> users = entry.getValue();
                if (!users.isEmpty()) {
                    stringRedisTemplate.opsForHyperLogLog().add(key, users.toArray(new String[0]));
                }
            }
        }
    }

    /**
     * 获取PV值
     *
     * @param key 键
     * @return PV值
     */
    public long getPv(String key) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                log.warn("Redis键 {} 的值不是有效数字: {}", key, value);
                return 0;
            }
        }
        return 0;
    }

    /**
     * 获取UV计数（使用HyperLogLog）
     *
     * @param key 键
     * @return UV计数
     */
    public long getUvCount(String key) {
        Long pfCount = stringRedisTemplate.opsForHyperLogLog().size(key);
        return pfCount != null ? pfCount : 0;
    }
} 
