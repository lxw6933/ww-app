package com.ww.app.pay.common;

import com.ww.app.pay.enums.PayChannelEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支付策略工厂
 */
@Component
public class PaymentStrategyFactory {
    
    private final Map<PayChannelEnum, PaymentStrategy> strategyMap = new HashMap<>();
    
    @Autowired
    private List<PaymentStrategy> strategies;
    
    @PostConstruct
    public void init() {
        strategies.forEach(strategy -> strategyMap.put(strategy.getPayChannel(), strategy));
    }
    
    /**
     * 获取支付策略
     * @param payChannel 支付渠道
     * @return 支付策略
     */
    public PaymentStrategy getStrategy(PayChannelEnum payChannel) {
        PaymentStrategy strategy = strategyMap.get(payChannel);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的支付渠道：" + payChannel);
        }
        return strategy;
    }
} 