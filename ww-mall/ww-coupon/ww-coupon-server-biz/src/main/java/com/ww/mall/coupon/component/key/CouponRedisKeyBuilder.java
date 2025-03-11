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

    public String buildCouponNumberKey(String activityCode) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, COUPON_NUMBER_KEY, activityCode);
    }

}
