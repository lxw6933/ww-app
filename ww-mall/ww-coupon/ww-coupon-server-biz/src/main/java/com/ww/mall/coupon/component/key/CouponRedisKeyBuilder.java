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

    private static final String SMS_COUPON_CODE_KEY = "sms_coupon_code";
    private static final String SMS_COUPON_NUMBER_KEY = "sms_coupon_number";

    /**
     * 获取券码对应的RSet key
     *
     * @param activityCode 活动编码
     * @param batchNo 批次号
     * @return key
     */
    public String buildCouponCodeKey(String activityCode, String batchNo) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, SMS_COUPON_CODE_KEY, activityCode, batchNo);
    }

    public String buildCouponNumberKey(String activityCode) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, SMS_COUPON_NUMBER_KEY, activityCode);
    }

    public String buildCouponCodeKeyPattern(String activityCode) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, SMS_COUPON_CODE_KEY, activityCode);
    }

}
