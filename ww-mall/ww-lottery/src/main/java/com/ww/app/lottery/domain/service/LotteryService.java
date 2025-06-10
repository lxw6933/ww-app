package com.ww.app.lottery.domain.service;

import com.ww.app.lottery.domain.context.LotteryContext;
import com.ww.app.lottery.domain.model.entity.LotteryActivity;
import com.ww.app.lottery.domain.result.LotteryResult;

/**
 * @author ww
 * @create 2025-06-06- 18:06
 * @description:
 */
public interface LotteryService {

    LotteryActivity getLotteryActivity(String activityCode);

    /**
     * 抽奖
     *
     * @param context context
     * @return LotteryResult
     */
    LotteryResult doLottery(LotteryContext context);

}
