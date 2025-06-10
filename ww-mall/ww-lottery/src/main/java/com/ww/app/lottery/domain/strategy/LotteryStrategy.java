package com.ww.app.lottery.domain.strategy;

import com.ww.app.common.exception.ApiException;
import com.ww.app.lottery.domain.context.LotteryContext;
import com.ww.app.lottery.domain.model.entity.LotteryActivity;
import com.ww.app.lottery.domain.result.LotteryResult;
import com.ww.app.lottery.enums.LotteryStrategyType;
import com.ww.app.lottery.utils.LotteryCacheUtils;

import java.util.Date;

public interface LotteryStrategy {

    /**
     * 执行抽奖
     *
     * @param context 抽奖上下文
     * @return 抽奖结果
     */
    LotteryResult draw(LotteryContext context);

    /**
     * 策略类型
     *
     * @return LotteryStrategyType
     */
    LotteryStrategyType support();

    default LotteryActivity getLotteryActivity(String activityCode) {
        LotteryActivity lotteryActivityCache = LotteryCacheUtils.getLotteryActivityCache(activityCode);
        if (lotteryActivityCache == null) {
            throw new ApiException("活动不存在");
        }
        if (!Boolean.TRUE.equals(lotteryActivityCache.getStatus())) {
            throw new ApiException("活动不已下架");
        }
        Date now = new Date();
        if (now.before(lotteryActivityCache.getStartTime()) || now.after(lotteryActivityCache.getEndTime())) {
            throw new ApiException("请求活动时间范围内参与");
        }
        return lotteryActivityCache;
    }
} 