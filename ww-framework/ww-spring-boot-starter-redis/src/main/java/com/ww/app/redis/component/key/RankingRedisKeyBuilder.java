package com.ww.app.redis.component.key;

import cn.hutool.core.util.StrUtil;
import com.ww.app.redis.key.RedisKeyBuilder;
import org.springframework.stereotype.Component;

/**
 * 排行榜 Redis Key 生成器
 * 
 * @author ww
 * @create 2025-11-25
 * @description: 排行榜功能相关的Redis Key生成器
 */
@Component
public class RankingRedisKeyBuilder extends RedisKeyBuilder {

    private static final String RANK_KEY = "rank";
    private static final String MEMBER_KEY = "member";

    /**
     * 生成排行榜的key
     * 
     * @param bizType 业务类型，用于区分不同的排行榜
     * @param bizId 业务ID，可选，用于区分同一业务类型下的不同排行榜
     * @return Redis key
     */
    public String buildRankingKey(String bizType, String bizId) {
        if (StrUtil.isNotBlank(bizId)) {
            return getPrefix() + StrUtil.join(SPLIT_ITEM, RANK_KEY, bizType, bizId);
        }
        return getPrefix() + StrUtil.join(SPLIT_ITEM, RANK_KEY, bizType);
    }

    /**
     * 生成排行榜的key（仅业务类型）
     * 
     * @param bizType 业务类型
     * @return Redis key
     */
    public String buildRankingKey(String bizType) {
        return buildRankingKey(bizType, null);
    }

    /**
     * 生成成员信息的key（用于存储额外信息）
     * 
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param memberId 成员ID
     * @return Redis key
     */
    public String buildMemberKey(String bizType, String bizId, String memberId) {
        if (StrUtil.isNotBlank(bizId)) {
            return getPrefix() + StrUtil.join(SPLIT_ITEM, RANK_KEY, MEMBER_KEY, bizType, bizId, memberId);
        }
        return getPrefix() + StrUtil.join(SPLIT_ITEM, RANK_KEY, MEMBER_KEY, bizType, memberId);
    }
}

