package com.ww.mall.promotion.constants;

/**
 * 促销活动缓存相关的 Redis 频道常量
 */
public final class RedisChannelConstant {

    /**
     * 拼团活动本地缓存变更通知频道
     * 发布 payload 为 activityId，监听方清理本地缓存
     */
    public static final String GROUP_ACTIVITY_CACHE_CHANNEL = "group:activity:cache:channel";
}

