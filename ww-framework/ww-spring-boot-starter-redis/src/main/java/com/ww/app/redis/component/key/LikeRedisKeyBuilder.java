package com.ww.app.redis.component.key;

import cn.hutool.core.util.StrUtil;
import com.ww.app.redis.key.RedisKeyBuilder;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-12-31
 * @description: 点赞功能相关的Redis Key生成器
 */
@Component
public class LikeRedisKeyBuilder extends RedisKeyBuilder {

    private static final String LIKE_KEY = "like";
    private static final String USER_KEY = "user";
    private static final String CONTENT_KEY = "content";
    private static final String COUNT_KEY = "count";
    private static final String HLL_KEY = "hll";
    private static final String BF_KEY = "bf";
    private static final String SHARDING_KEY = "sharding";
    private static final String RANK_KEY = "rank";

    /**
     * 生成用户点赞集合的key
     */
    public String buildUserLikeKey(String userId) {
        return getPrefix() + StrUtil.join(SPLIT_ITEM, LIKE_KEY, USER_KEY, userId);
    }

    /**
     * 生成内容点赞集合的key
     */
    public String buildContentLikeKey(String contentType, String contentId, Integer shardId) {
        if (shardId != null) {
            return getPrefix() + StrUtil.join(SPLIT_ITEM, LIKE_KEY, CONTENT_KEY, contentType, contentId, shardId);
        }
        return getPrefix() + StrUtil.join(SPLIT_ITEM, LIKE_KEY, CONTENT_KEY, contentType, contentId);
    }

    /**
     * 生成内容分片信息的key
     */
    public String buildShardingKey(String contentType, String contentId) {
        return getPrefix() + StrUtil.join(SPLIT_ITEM, LIKE_KEY, SHARDING_KEY, contentType, contentId);
    }

    /**
     * 生成点赞计数的key
     */
    public String buildCountKey(String contentType, String contentId) {
        return getPrefix() + StrUtil.join(SPLIT_ITEM, LIKE_KEY, COUNT_KEY, contentType, contentId);
    }

    /**
     * 生成HyperLogLog计数的key
     */
    public String buildHllKey(String contentType, String contentId) {
        return getPrefix() + StrUtil.join(SPLIT_ITEM, LIKE_KEY, HLL_KEY, contentType, contentId);
    }

    /**
     * 生成布隆过滤器的key
     */
    public String buildBfKey(String contentType, String contentId) {
        return getPrefix() + StrUtil.join(SPLIT_ITEM, LIKE_KEY, BF_KEY, contentType, contentId);
    }

    /**
     * 生成热门排行榜的key
     */
    public String buildRankKey(String contentType) {
        return getPrefix() + StrUtil.join(SPLIT_ITEM, LIKE_KEY, RANK_KEY, contentType);
    }
}