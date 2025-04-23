package com.ww.app.redis.component.key;

import org.springframework.stereotype.Component;

/**
 * 短码Redis键构建器
 * 用于构建分布式短码生成相关的Redis键
 *
 * @author ww
 */
@Component
public class ShortCodeRedisKeyBuilder {

    /**
     * Redis键前缀 - 最大ID
     */
    private static final String MAX_ID_KEY_PREFIX = "short:code:segment:maxid:";

    /**
     * Redis键前缀 - 分布式锁
     */
    private static final String LOCK_KEY_PREFIX = "short:code:segment:lock:";

    /**
     * 构建最大ID的Redis键
     *
     * @param businessType 业务类型
     * @param length       短码长度
     * @return 最大ID的Redis键
     */
    public String buildMaxIdKey(String businessType, int length) {
        return MAX_ID_KEY_PREFIX + businessType + ":" + length;
    }

    /**
     * 构建分布式锁的Redis键
     *
     * @param businessType 业务类型
     * @param length       短码长度
     * @return 分布式锁的Redis键
     */
    public String buildLockKey(String businessType, int length) {
        return LOCK_KEY_PREFIX + businessType + ":" + length;
    }
} 