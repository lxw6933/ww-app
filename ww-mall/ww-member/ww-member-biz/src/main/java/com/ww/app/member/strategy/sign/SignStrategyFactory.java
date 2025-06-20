package com.ww.app.member.strategy.sign;

import com.ww.app.member.enums.SignPeriodEnum;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
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

    @Resource
    private List<SignStrategy> signStrategies;

    private final Map<SignPeriodEnum, SignStrategy> strategyMap = new HashMap<>();

    @PostConstruct
    public void init() {
        for (SignStrategy strategy : signStrategies) {
            strategyMap.put(strategy.getType(), strategy);
        }
    }

    /**
     * 获取签到策略
     *
     * @param type 签到类型
     * @return 签到策略
     */
    public SignStrategy getStrategy(SignPeriodEnum type) {
        SignStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            // 默认使用月签到策略
            return strategyMap.get(SignPeriodEnum.MONTHLY);
        }
        return strategy;
    }

    public SignStrategy getDefaultStrategy() {
        return strategyMap.get(SignPeriodEnum.MONTHLY);
    }
} 