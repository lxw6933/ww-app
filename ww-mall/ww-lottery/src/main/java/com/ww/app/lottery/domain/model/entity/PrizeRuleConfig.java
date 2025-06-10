package com.ww.app.lottery.domain.model.entity;

import lombok.Data;

/**
 * @author ww
 * @create 2025-06-10- 14:05
 * @description:
 */
@Data
public class PrizeRuleConfig {

    /** 每人中奖该奖品的次数 */
    private int lotteryPrizeCount;
    /** 每人保底需要抽奖多少次才能中该奖品 */
    private int leastDrawLimit;

}
