package com.ww.app.lottery.domain.strategy.impl;

import com.ww.app.lottery.domain.context.LotteryContext;
import com.ww.app.lottery.domain.model.entity.LotteryActivity;
import com.ww.app.lottery.domain.model.entity.PrizeConfig;
import com.ww.app.lottery.domain.result.LotteryResult;
import com.ww.app.lottery.domain.strategy.LotteryStrategy;
import com.ww.app.lottery.enums.LotteryStrategyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WeightRandomLotteryStrategy implements LotteryStrategy {

    @Override
    public LotteryResult draw(LotteryContext context) {
        LotteryActivity lotteryActivity = getLotteryActivity(context.getActivityCode());
        // 计算总权重
        double totalWeight = lotteryActivity.getPrizes().stream()
                .mapToDouble(PrizeConfig::getProbability)
                .sum();
        // TODO 待完善

        return null;
    }

    @Override
    public LotteryStrategyType support() {
        return LotteryStrategyType.WEIGHT;
    }

}