package com.ww.app.member.strategy.sign;

import com.ww.app.member.enums.SignPeriodEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ww
 * @create 2023-07-21- 09:16
 * @description: 签到策略工厂
 */
@Component
public class SignStrategyFactory {

    @Autowired
    private List<AbstractSignStrategy> signStrategies;

    private final Map<SignPeriodEnum, AbstractSignStrategy> strategyMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (AbstractSignStrategy strategy : signStrategies) {
            strategyMap.put(strategy.getType(), strategy);
        }
    }

    /**
     * 获取签到策略
     *
     * @param type 签到类型
     * @return 签到策略
     */
    public AbstractSignStrategy getStrategy(SignPeriodEnum type) {
        AbstractSignStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            // 默认使用月签到策略
            return strategyMap.get(SignPeriodEnum.MONTHLY);
        }
        return strategy;
    }

    public AbstractSignStrategy getDefaultStrategy() {
        return strategyMap.get(SignPeriodEnum.MONTHLY);
    }
} 