package com.ww.app.lottery.domain.model.entity;

import com.ww.app.lottery.enums.PrizeType;
import lombok.Data;

import java.util.Map;

/**
 * @author ww
 * @create 2025-06-09- 14:22
 * @description: 奖品配置信息
 */
@Data
public class PrizeConfig {
    /** 奖品ID */
    private String prizeId;
    
    /** 奖品类型 */
    private PrizeType prizeType;

    /** 礼品id */
    private String lotteryId;
    
    /** 奖品名称 */
    private String prizeName;

    /** 发放数量 */
    private int issueNumber;

    /** 奖品库存 */
    private int stockCount;
    
    /** 概率（权重） */
    private Double probability;

    /** 奖品规则配置 */
    private PrizeRuleConfig prizeRuleConfig;

    /** 额外信息 */
    private Map<String, Object> extraInfo;
} 