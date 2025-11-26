package com.ww.app.redis.component.rank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 排行榜项
 * 
 * @author ww
 * @create 2025-11-25 9:08
 * @description: 排行榜数据项，包含成员ID、分数、排名等信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 成员ID（用户ID或其他业务标识）
     */
    private String memberId;

    /**
     * 分数
     */
    private Double score;

    /**
     * 排名（从1开始，0表示未上榜）
     */
    private Long rank;

    /**
     * 更新时间戳（毫秒）
     */
    private Long updateTime;

    /**
     * 扩展数据（JSON格式，用于存储额外信息）
     */
    private String extraData;
}
