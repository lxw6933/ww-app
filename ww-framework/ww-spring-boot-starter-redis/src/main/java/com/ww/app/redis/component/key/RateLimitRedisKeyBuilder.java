package com.ww.app.redis.component.key;

import cn.hutool.core.util.StrUtil;
import com.ww.app.redis.key.RedisKeyBuilder;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2023-09-05- 10:40
 * @description: 限流Redis Key构建器
 */
@Component
public class RateLimitRedisKeyBuilder extends RedisKeyBuilder {

    /**
     * 限流key前缀
     */
    public static final String RATE_LIMIT_PREFIX = "rate:limit";
    
    /**
     * 白名单key前缀
     */
    public static final String WHITELIST_PREFIX = "rate:whitelist";
    
    /**
     * 黑名单key前缀
     */
    public static final String BLACKLIST_PREFIX = "rate:blacklist";

    /**
     * 构建限流key
     *
     * @param type 限流类型
     * @param className 类名
     * @param methodName 方法名
     * @return Redis key
     */
    public String buildRateLimitKey(String type, String className, String methodName) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, RATE_LIMIT_PREFIX, type, className, methodName);
    }
    
    /**
     * 构建IP限流key
     *
     * @param type 限流类型
     * @param className 类名
     * @param methodName 方法名
     * @param ip IP地址
     * @return Redis key
     */
    public String buildIpRateLimitKey(String type, String className, String methodName, String ip) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, RATE_LIMIT_PREFIX, type, "ip", ip, className, methodName);
    }
    
    /**
     * 构建用户限流key
     *
     * @param type 限流类型
     * @param className 类名
     * @param methodName 方法名
     * @param userId 用户ID
     * @return Redis key
     */
    public String buildUserRateLimitKey(String type, String className, String methodName, String userId) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, RATE_LIMIT_PREFIX, type, "user", userId, className, methodName);
    }
    
    /**
     * 构建IP和用户组合限流key
     *
     * @param type 限流类型
     * @param className 类名
     * @param methodName 方法名
     * @param ip IP地址
     * @param userId 用户ID
     * @return Redis key
     */
    public String buildIpUserRateLimitKey(String type, String className, String methodName, String ip, String userId) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, RATE_LIMIT_PREFIX, type, "ip_user", ip, userId, className, methodName);
    }
    
    /**
     * 构建白名单key
     *
     * @return Redis key
     */
    public String buildWhitelistKey() {
        return super.getPrefix() + WHITELIST_PREFIX;
    }
    
    /**
     * 构建黑名单key
     *
     * @return Redis key
     */
    public String buildBlacklistKey() {
        return super.getPrefix() + BLACKLIST_PREFIX;
    }
} 