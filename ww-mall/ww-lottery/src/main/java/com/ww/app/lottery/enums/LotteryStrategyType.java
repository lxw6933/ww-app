package com.ww.app.lottery.enums;

import lombok.Getter;

/**
 * @author ww
 * @create 2025-06-06- 14:22
 * @description:
 */
@Getter
public enum LotteryStrategyType {

    RANDOM("随机抽奖", "random"),
    PROBABILITY("概率抽奖", "probability"),
    TIME_RANGE("时间段抽奖", "timeRange"),
    ANNUAL_MEETING("年会抽奖", "annualMeeting"),
    WEIGHT("权重抽奖", "weight"),
    LEVEL("等级抽奖", "level");

    private final String description;
    private final String code;

    LotteryStrategyType(String description, String code) {
        this.description = description;
        this.code = code;
    }

    public static LotteryStrategyType fromCode(String code) {
        for (LotteryStrategyType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid StrategyType code: " + code);
    }

}
