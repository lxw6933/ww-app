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

    public String buildCouponCodePrefixKey() {
        return super.getPrefix() + COUPON_CODE_KEY + SPLIT_ITEM;
    }

    /**
     * 获取券码对应的RSet key
     *
     * @param activityCode 活动编码
     * @param batchNo 批次号
     * @return key
     */
    public String buildCouponCodeKey(String activityCode, String batchNo) {
        return buildCouponCodePrefixKey() + StrUtil.join(SPLIT_ITEM, activityCode, batchNo);
    }

    public String buildCouponNumberPrefixKey() {
        return super.getPrefix() + COUPON_NUMBER_KEY + SPLIT_ITEM;
    }

    /**
     * 获取优惠券活动库存数量key
     *
     * @param activityCode 活动编码
     * @return key
     */
    public String buildCouponNumberKey(String activityCode) {
        return buildCouponNumberPrefixKey() + activityCode;
    }

    /**
     * 从 key 中提取 activityCode
     */
    public String extractActivityCode(String fullKey, String keyPrefix) {
        if (!StrUtil.isNotBlank(fullKey) || !fullKey.startsWith(keyPrefix)) {
            return null;
        }
        // 移除前缀后，剩余部分格式应为 "activityCode" 或 "activityCode:batchNo"
        String suffix = fullKey.substring(keyPrefix.length());
        String[] parts = suffix.split(SPLIT_ITEM);
        // 第一个部分是 activityCode
        return (parts.length > 0) ? parts[0] : null;
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
