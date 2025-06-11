package com.ww.app.lottery.domain.strategy.impl;

import com.ww.app.lottery.domain.context.LotteryContext;
import com.ww.app.lottery.domain.model.entity.LotteryActivity;
import com.ww.app.lottery.domain.model.entity.PrizeConfig;
import com.ww.app.lottery.domain.result.LotteryResult;
import com.ww.app.lottery.domain.strategy.LotteryStrategy;
import com.ww.app.lottery.enums.LotteryStrategyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
public class WeightRandomLotteryStrategy implements LotteryStrategy {

    @Override
    public LotteryResult draw(LotteryContext context) {
        LotteryActivity lotteryActivity = getLotteryActivity(context.getActivityCode());
        // 计算总权重
        double totalProbability = lotteryActivity.getPrizes().stream()
                .mapToDouble(PrizeConfig::getProbability)
                .sum();
        if (totalProbability > 100) {
            log.error("奖品总概率大于100");
            throw new IllegalArgumentException("Total probability cannot exceed 100%. Current total: " + totalProbability);
        }
        List<LotteryActivity.ProbabilityRange> probabilityRanges = lotteryActivity.getProbabilityRanges();
        // 生成[0,100)之间的随机数
        double randomValue = ThreadLocalRandom.current().nextDouble() * 100.0;
        // 检查是否落在某个区间
        PrizeConfig prize = null;
        for (LotteryActivity.ProbabilityRange range : probabilityRanges) {
            if (randomValue >= range.getStart() && randomValue < range.getEnd()) {
                prize = range.getPrize();
                break;
            }
        }
        LotteryResult lotteryResult = new LotteryResult();
        lotteryResult.setDrawTime(new Date());
        if (prize == null) {
            // 未中奖
            lotteryResult.setSuccess(false);
            lotteryResult.setMessage(LotteryResult.ResultCode.FAIL.getMessage());
            lotteryResult.setResultCode(LotteryResult.ResultCode.FAIL);
        } else {
            // 中奖
            lotteryResult.setSuccess(true);
            lotteryResult.setPrizeId(prize.getPrizeId());
            lotteryResult.setPrizeName(prize.getPrizeName());
            lotteryResult.setPrizeType(prize.getPrizeType());
            lotteryResult.setPrizeAmount(prize.getIssueNumber());
            lotteryResult.setMessage(LotteryResult.ResultCode.SUCCESS.getMessage());
            lotteryResult.setResultCode(LotteryResult.ResultCode.SUCCESS);
        }
        return lotteryResult;
    }

    @Override
    public LotteryStrategyType support() {
        return LotteryStrategyType.WEIGHT;
    }

}