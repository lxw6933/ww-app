package com.ww.mall.coupon.component.key;

import cn.hutool.core.util.StrUtil;
import com.ww.app.redis.key.RedisKeyBuilder;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2025-03-06- 18:15
 * @description:
 */
@Component
public class CouponRedisKeyBuilder extends RedisKeyBuilder {

    private static final String COUPON_CODE_KEY = "coupon_code";
    private static final String COUPON_NUMBER_KEY = "coupon_number";
    private static final String COUPON_FREEZE_KEY = "coupon_freeze";

    /**
     * 获取券码对应的RSet key
     *
     * @param activityCode 活动编码
     * @param batchNo 批次号
     * @return key
     */
    public String buildCouponCodeKey(String activityCode, String batchNo) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, COUPON_CODE_KEY, activityCode, batchNo);
    }

    /**
     * 获取优惠券活动库存数量key
     *
     * @param activityCode 活动编码
     * @return key
     */
    public String buildCouponNumberKey(String activityCode) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, COUPON_NUMBER_KEY, activityCode);
    }

    /**
     * 获取冻结用户优惠券key
     *
     * @param couponRecordId 用户优惠券id
     * @return key
     */
    public String buildCouponFreezeKey(String couponRecordId) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, COUPON_FREEZE_KEY, couponRecordId);
    }

}
