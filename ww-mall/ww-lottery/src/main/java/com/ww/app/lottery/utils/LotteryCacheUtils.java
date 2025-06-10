package com.ww.app.lottery.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.ww.app.common.utils.CaffeineUtil;
import com.ww.app.lottery.domain.model.entity.LotteryActivity;
import com.ww.app.mongodb.utils.MongoUtils;

import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2025-06-06- 18:13
 * @description:
 */
public class LotteryCacheUtils {

    private LotteryCacheUtils() {}

    private static final Cache<String, LotteryActivity> lotteryActivityCache = CaffeineUtil.createCache(100, 200, 30, TimeUnit.MINUTES);

    public static LotteryActivity getLotteryActivityCache(String activityCode) {
        return lotteryActivityCache.get(activityCode, code -> MongoUtils.getMongoTemplate().findOne(LotteryActivity.buildActivityCodeQuery(code), LotteryActivity.class));
    }

    public static void updateLotteryActivityCache(String activityCode) {
        lotteryActivityCache.invalidate(activityCode);
    }

}
