package com.ww.app.lottery.domain.strategy.impl;

import com.ww.app.lottery.domain.context.LotteryContext;
import com.ww.app.lottery.domain.result.LotteryResult;
import com.ww.app.lottery.domain.strategy.LotteryStrategy;
import com.ww.app.lottery.enums.LotteryStrategyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author ww
 * @create 2025-06-10- 14:47
 * @description:
 */
@Slf4j
@Component
public class AnnualMeetingStrategy implements LotteryStrategy {

    @Override
    public LotteryResult draw(LotteryContext context) {
        Map<String, Object> params = context.getParams();
        // 中奖奖品【文本类型】
        int lotteryName = (int) params.get("lotteryName");
        // 中奖人数
        int lotteryNumber = (int) params.get("lotteryNumber");
        // TODO 待完善
        return null;
    }

    @Override
    public LotteryStrategyType support() {
        return LotteryStrategyType.ANNUAL_MEETING;
    }
}
