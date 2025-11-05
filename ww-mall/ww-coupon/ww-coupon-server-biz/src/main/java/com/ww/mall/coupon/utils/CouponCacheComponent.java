package com.ww.mall.coupon.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.CaffeineUtil;
import com.ww.mall.coupon.entity.SmsCouponActivity;
import com.ww.mall.coupon.eunms.ErrorCodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2025-03-21- 16:51
 * @description:
 */
@Slf4j
@Component
public class CouponCacheComponent {

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 优惠券活动缓存
     * 容量: 100-200
     * 过期时间: 30分钟
     */
    private static final Cache<String, SmsCouponActivity> smsCouponActivityCache = CaffeineUtil.createCache(100, 200, 30, TimeUnit.MINUTES);

    /**
     * 空值缓存（防止缓存穿透）
     * 容量: 1000
     * 过期时间: 5分钟
     */
    private static final Cache<String, Boolean> NULL_CACHE = CaffeineUtil.createCache(500, 1000, 5, TimeUnit.MINUTES);

    public SmsCouponActivity getSmsCouponActivityCache(String activityCode) {
        // 1. 检查空值缓存
        Boolean isNull = NULL_CACHE.getIfPresent(activityCode);
        if (Boolean.TRUE.equals(isNull)) {
            log.debug("命中空值缓存，活动不存在: {}", activityCode);
            throw new ApiException(ErrorCodeConstants.UN_FOUND_ACTIVITY);
        }
        SmsCouponActivity smsCouponActivity = smsCouponActivityCache.get(activityCode, code -> {
            log.debug("缓存未命中，查询数据库: {}", code);
            SmsCouponActivity result = mongoTemplate.findOne(SmsCouponActivity.buildActivityCodeQuery(code), SmsCouponActivity.class);
            // 如果不存在，加入空值缓存
            if (result == null) {
                NULL_CACHE.put(code, Boolean.TRUE);
                log.warn("活动不存在，加入空值缓存: {}", code);
            }
            return result;
        });

        if (smsCouponActivity == null) {
            throw new ApiException(ErrorCodeConstants.UN_FOUND_ACTIVITY);
        }
        return smsCouponActivity;
    }

    /**
     * 刷新活动缓存
     *
     * @param activityCode 活动编码
     */
    public void updateSmsCouponActivityCache(String activityCode) {
        smsCouponActivityCache.invalidate(activityCode);
        NULL_CACHE.invalidate(activityCode);
    }

}
