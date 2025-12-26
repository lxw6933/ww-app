package com.ww.app.open.utils;

import com.ww.app.open.constant.OpenPlatformConstants;

/**
 * 缓存Key构建工具类
 * 
 * @author ww
 * @create 2024-05-27
 * @description: 统一管理缓存key的构建，避免硬编码
 */
public class CacheKeyBuilder {

    private CacheKeyBuilder() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 构建防重放攻击缓存key
     * 
     * @param transId 流水号
     * @return 缓存key
     */
    public static String buildReplayKey(String transId) {
        return OpenPlatformConstants.CACHE_KEY_REPLAY_PREFIX + transId;
    }

    /**
     * 构建限流缓存key
     * 
     * @param appCode 应用编码
     * @param apiCode API编码
     * @return 缓存key
     */
    public static String buildRateLimitKey(String appCode, String apiCode) {
        return OpenPlatformConstants.CACHE_KEY_RATE_LIMIT_PREFIX + appCode + 
               OpenPlatformConstants.KEY_SEPARATOR + apiCode;
    }

    /**
     * 构建API信息缓存key（按apiCode）
     * 
     * @param apiCode API编码
     * @return 缓存key
     */
    public static String buildApiKey(String apiCode) {
        return OpenPlatformConstants.CACHE_KEY_API_PREFIX + apiCode;
    }

    /**
     * 构建API路径缓存key
     * 
     * @param apiPath API路径
     * @param httpMethod HTTP方法
     * @return 缓存key
     */
    public static String buildApiPathKey(String apiPath, String httpMethod) {
        return apiPath + OpenPlatformConstants.KEY_SEPARATOR + httpMethod;
    }

    /**
     * 构建权限缓存key
     * 
     * @param appCode 应用编码
     * @param apiCode API编码
     * @return 缓存key
     */
    public static String buildPermissionKey(String appCode, String apiCode) {
        return appCode + OpenPlatformConstants.KEY_SEPARATOR + apiCode;
    }

    /**
     * 构建配置缓存key
     * 
     * @param configKey 配置key
     * @return 缓存key
     */
    public static String buildConfigKey(String configKey) {
        return OpenPlatformConstants.CACHE_KEY_CONFIG_PREFIX + configKey;
    }

    /**
     * 构建应用缓存key
     * 
     * @param appCode 应用编码
     * @return 缓存key
     */
    public static String buildAppKey(String appCode) {
        return OpenPlatformConstants.CACHE_KEY_APP_PREFIX + appCode;
    }
}

