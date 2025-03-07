package com.ww.mall.coupon.utils;

import cn.hutool.core.util.StrUtil;

/**
 * @author ww
 * @create 2025-01-16- 17:00
 * @description:
 */
public class CouponUtils {

    private CouponUtils() {}

    private static final String SMS_COUPON_CODE_DOC = "sms_coupon_code";
    private static final String SMS_COUPON_RECORD_DOC = "sms_coupon_record";

    public static String getSmsCouponCodeCollectionName(long channelId) {
        return StrUtil.join(StrUtil.UNDERLINE, SMS_COUPON_CODE_DOC, (int) channelId);
    }

    public static String getSmsCouponRecordCollectionName(long channelId) {
        return StrUtil.join(StrUtil.UNDERLINE, SMS_COUPON_RECORD_DOC, (int) channelId);
    }

}
