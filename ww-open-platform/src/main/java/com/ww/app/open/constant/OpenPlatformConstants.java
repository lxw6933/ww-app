package com.ww.app.open.constant;

/**
 * 开放平台常量类
 * 
 * @author ww
 * @create 2024-05-27
 * @description: 统一管理开放平台模块的常量，避免魔法值
 */
public class OpenPlatformConstants {

    private OpenPlatformConstants() {
        throw new IllegalStateException("Utility class");
    }

    // ==================== 状态常量 ====================
    
    /**
     * 启用状态
     */
    public static final Integer STATUS_ENABLED = 1;
    
    /**
     * 禁用状态
     */
    public static final Integer STATUS_DISABLED = 0;

    // ==================== 缓存Key前缀 ====================
    
    /**
     * 防重放攻击缓存key前缀
     */
    public static final String CACHE_KEY_REPLAY_PREFIX = "open:replay:";
    
    /**
     * 限流缓存key前缀
     */
    public static final String CACHE_KEY_RATE_LIMIT_PREFIX = "open:ratelimit:";
    
    /**
     * API信息缓存key前缀
     */
    public static final String CACHE_KEY_API_PREFIX = "open:api:";
    
    /**
     * API路径缓存key前缀
     */
    public static final String CACHE_KEY_API_PATH_PREFIX = "open:api:path:";
    
    /**
     * 权限缓存key前缀
     */
    public static final String CACHE_KEY_PERMISSION_PREFIX = "open:permission:";
    
    /**
     * 配置缓存key前缀
     */
    public static final String CACHE_KEY_CONFIG_PREFIX = "open:config:";
    
    /**
     * 应用缓存key前缀
     */
    public static final String CACHE_KEY_APP_PREFIX = "open:app:";

    // ==================== 分隔符 ====================
    
    /**
     * 缓存key分隔符
     */
    public static final String KEY_SEPARATOR = ":";
    
    /**
     * IP白名单分隔符
     */
    public static final String IP_WHITELIST_SEPARATOR = ",";

    // ==================== 时间相关常量 ====================
    
    /**
     * 请求时间格式
     */
    public static final String REQUEST_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    
    /**
     * 请求时间允许的最大偏差（秒）
     */
    public static final long MAX_TIME_DEVIATION_SECONDS = 300L;
    
    /**
     * 防重放攻击的缓存时间（秒）
     */
    public static final long REPLAY_ATTACK_CACHE_TIME_SECONDS = 300L;
    
    /**
     * 限流窗口时间（秒）
     */
    public static final long RATE_LIMIT_WINDOW_SECONDS = 60L;

    // ==================== 缓存配置常量 ====================
    
    /**
     * 防重放缓存初始容量
     */
    public static final int REPLAY_CACHE_INITIAL_CAPACITY = 1000;
    
    /**
     * 防重放缓存最大容量
     */
    public static final int REPLAY_CACHE_MAXIMUM_SIZE = 10000;
    
    /**
     * 限流缓存初始容量
     */
    public static final int RATE_LIMIT_CACHE_INITIAL_CAPACITY = 500;
    
    /**
     * 限流缓存最大容量
     */
    public static final int RATE_LIMIT_CACHE_MAXIMUM_SIZE = 5000;
    
    /**
     * API信息缓存初始容量
     */
    public static final int API_CACHE_INITIAL_CAPACITY = 200;
    
    /**
     * API信息缓存最大容量
     */
    public static final int API_CACHE_MAXIMUM_SIZE = 1000;
    
    /**
     * API信息缓存过期时间（分钟）
     */
    public static final int API_CACHE_EXPIRE_MINUTES = 60;
    
    /**
     * API信息缓存刷新时间（分钟）
     */
    public static final int API_CACHE_REFRESH_MINUTES = 30;
    
    /**
     * 权限缓存初始容量
     */
    public static final int PERMISSION_CACHE_INITIAL_CAPACITY = 1000;
    
    /**
     * 权限缓存最大容量
     */
    public static final int PERMISSION_CACHE_MAXIMUM_SIZE = 5000;
    
    /**
     * 权限缓存过期时间（分钟）
     */
    public static final int PERMISSION_CACHE_EXPIRE_MINUTES = 60;
    
    /**
     * 权限缓存刷新时间（分钟）
     */
    public static final int PERMISSION_CACHE_REFRESH_MINUTES = 30;
    
    /**
     * 配置缓存初始容量
     */
    public static final int CONFIG_CACHE_INITIAL_CAPACITY = 200;
    
    /**
     * 配置缓存最大容量
     */
    public static final int CONFIG_CACHE_MAXIMUM_SIZE = 1000;
    
    /**
     * 配置缓存过期时间（分钟）
     */
    public static final int CONFIG_CACHE_EXPIRE_MINUTES = 60;
    
    /**
     * 配置缓存刷新时间（分钟）
     */
    public static final int CONFIG_CACHE_REFRESH_MINUTES = 30;
    
    /**
     * 空值缓存初始容量
     */
    public static final int NULL_VALUE_CACHE_INITIAL_CAPACITY = 100;
    
    /**
     * 空值缓存最大容量
     */
    public static final int NULL_VALUE_CACHE_MAXIMUM_SIZE = 500;
    
    /**
     * 空值缓存过期时间（分钟）
     */
    public static final int NULL_VALUE_CACHE_EXPIRE_MINUTES = 5;
    
    /**
     * 应用缓存初始容量
     */
    public static final int APPLICATION_CACHE_INITIAL_CAPACITY = 200;
    
    /**
     * 应用缓存最大容量
     */
    public static final int APPLICATION_CACHE_MAXIMUM_SIZE = 1000;
    
    /**
     * 应用缓存过期时间（分钟）
     */
    public static final int APPLICATION_CACHE_EXPIRE_MINUTES = 60;
    
    /**
     * 应用缓存刷新时间（分钟）
     */
    public static final int APPLICATION_CACHE_REFRESH_MINUTES = 30;
}

