package com.ww.app.lottery.domain.model.entity;

import lombok.Data;
import lombok.Getter;

import java.util.List;

/**
 * @author ww
 * @create 2025-06-10- 14:05
 * @description:
 */
@Data
public class LotteryRuleConfig {

    /** 每人最大抽奖次数 */
    private int maxDrawCount;
    /** 每人每日抽奖限制 */
    private int dailyDrawLimit;
    /** 每人最大中奖限制 */
    private int maxLotteryLimit;
    /** 每人每日中奖限制 */
    private int dailyLotteryLimit;
    /** 允许的用户等级 */
    private List<UserLevel> allowLevels;

    @Getter
    public enum UserLevel {
        NORMAL(1, "普通用户"),
        VIP(2, "VIP用户"),
        SVIP(3, "超级VIP用户");

        private final int level;
        private final String desc;

        UserLevel(int level, String desc) {
            this.level = level;
            this.desc = desc;
        }
    }

}
