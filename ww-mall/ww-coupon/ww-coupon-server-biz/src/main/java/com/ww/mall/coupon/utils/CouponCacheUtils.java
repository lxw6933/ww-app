package com.ww.mall.coupon.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.ww.app.common.utils.CaffeineUtil;
import com.ww.app.mongodb.utils.MongoUtils;
import com.ww.mall.coupon.entity.SmsCouponActivity;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2025-03-21- 16:51
 * @description:
 */
@Slf4j
public class CouponCacheUtils {

    private CouponCacheUtils() {}

    private static final Cache<String, SmsCouponActivity> smsCouponActivityCache = CaffeineUtil.createCache(100, 200, 30, TimeUnit.MINUTES);

    public static SmsCouponActivity getSmsCouponActivityCache(String activityCode) {
        return smsCouponActivityCache.get(activityCode, code -> MongoUtils.getMongoTemplate().findOne(SmsCouponActivity.buildActivityCodeQuery(code), SmsCouponActivity.class));
    }

    public static void updateSmsCouponActivityCache(String activityCode) {
        smsCouponActivityCache.invalidate(activityCode);
    }

}
