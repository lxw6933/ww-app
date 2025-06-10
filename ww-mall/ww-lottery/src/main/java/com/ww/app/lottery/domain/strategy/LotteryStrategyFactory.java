package com.ww.app.lottery.domain.strategy;

import com.ww.app.lottery.enums.LotteryStrategyType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author ww
 * @create 2025-06-06- 15:16
 * @description:
 */
@Component
public class LotteryStrategyFactory {

    private final Map<LotteryStrategyType, LotteryStrategy> lotteryStrategyMap;

    public LotteryStrategyFactory(List<LotteryStrategy> lotteryStrategyList) {
        this.lotteryStrategyMap = lotteryStrategyList.stream()
                .collect(Collectors.toMap(LotteryStrategy::support, p -> p));
    }

    public LotteryStrategy getLotteryStrategy(LotteryStrategyType type) {
        LotteryStrategy strategy = lotteryStrategyMap.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported strategy type: " + type);
        }
        return strategy;
    }

}
