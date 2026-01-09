package com.ww.mall.coupon.utils;

import cn.hutool.core.lang.Assert;
import com.github.benmanes.caffeine.cache.Cache;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.CaffeineUtil;
import com.ww.mall.coupon.entity.MerchantCouponActivity;
import com.ww.mall.coupon.entity.SmsCouponActivity;
import com.ww.mall.coupon.enums.CouponType;
import com.ww.mall.coupon.enums.ErrorCodeConstants;
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

    private static final Cache<String, MerchantCouponActivity> merchantCouponActivityCache = CaffeineUtil.createCache(100, 200, 30, TimeUnit.MINUTES);

    /**
     * 空值缓存（防止缓存穿透）
     * 容量: 1000
     * 过期时间: 5分钟
     */
    private static final Cache<String, Boolean> NULL_CACHE = CaffeineUtil.createCache(500, 1000, 5, TimeUnit.MINUTES);

    public SmsCouponActivity getSmsCouponActivityCache(String activityCode) {
        Assert.isTrue(CouponType.PLATFORM.equals(CouponUtils.getCouponType(activityCode)), () -> new ApiException(ErrorCodeConstants.DATA_ERROR));
        // 1. 检查空值缓存
        checkNullCache(activityCode);
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

    private static void checkNullCache(String activityCode) {
        Boolean isNull = NULL_CACHE.getIfPresent(activityCode);
        if (Boolean.TRUE.equals(isNull)) {
            log.debug("命中空值缓存，活动不存在: {}", activityCode);
            throw new ApiException(ErrorCodeConstants.UN_FOUND_ACTIVITY);
        }
    }

    public MerchantCouponActivity getMerchantCouponActivityCache(String activityCode) {
        Assert.isTrue(CouponType.MERCHANT.equals(CouponUtils.getCouponType(activityCode)), () -> new ApiException(ErrorCodeConstants.DATA_ERROR));
        // 1. 检查空值缓存
        checkNullCache(activityCode);
        MerchantCouponActivity merchantCouponActivity = merchantCouponActivityCache.get(activityCode, code -> {
            log.debug("缓存未命中，查询数据库: {}", code);
            MerchantCouponActivity result = mongoTemplate.findOne(MerchantCouponActivity.buildActivityCodeQuery(code), MerchantCouponActivity.class);
            // 如果不存在，加入空值缓存
            if (result == null) {
                NULL_CACHE.put(code, Boolean.TRUE);
                log.warn("活动不存在，加入空值缓存: {}", code);
            }
            return result;
        });

        if (merchantCouponActivity == null) {
            throw new ApiException(ErrorCodeConstants.UN_FOUND_ACTIVITY);
        }
        return merchantCouponActivity;
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

    public void updateMerchantCouponActivityCache(String activityCode) {
        merchantCouponActivityCache.invalidate(activityCode);
        NULL_CACHE.invalidate(activityCode);
    }

}
