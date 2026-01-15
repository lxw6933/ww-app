package com.ww.mall.coupon.utils;

import cn.hutool.core.util.StrUtil;
import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.IdUtil;
import com.ww.mall.coupon.enums.CouponType;
import com.ww.mall.coupon.enums.ErrorCodeConstants;

/**
 * @author ww
 * @create 2025-01-16- 17:00
 * @description:
 */
public class CouponUtils {

    private CouponUtils() {}

    public static final String SMS_COUPON_PREFIX = "SC";
    public static final String MERCHANT_COUPON_PREFIX = "MC";

    public static String getSmsCouponCode() {
        return StrUtil.join(StrUtil.EMPTY, SMS_COUPON_PREFIX, IdUtil.nextIdStr());
    }

    public static String getMerchantCouponCode() {
        return StrUtil.join(StrUtil.EMPTY, MERCHANT_COUPON_PREFIX, IdUtil.nextIdStr());
    }

    public static String getCouponActivityCode(CouponType couponType) {
        switch (couponType) {
            case MERCHANT:
                return getMerchantCouponCode();
            case PLATFORM:
                return getSmsCouponCode();
            default:
                throw new ApiException(ErrorCodeConstants.DATA_ERROR);
        }
    }

    private static final String SMS_COUPON_CODE_DOC = "sms_coupon_code";
    private static final String SMS_COUPON_RECORD_DOC = "sms_coupon_record";

    public static String getSmsCouponCodeCollectionName(long channelId) {
        return StrUtil.join(StrUtil.UNDERLINE, SMS_COUPON_CODE_DOC, (int) channelId);
    }

    public static String getSmsCouponRecordCollectionName(long channelId) {
        return StrUtil.join(StrUtil.UNDERLINE, SMS_COUPON_RECORD_DOC, (int) channelId);
    }

    public static CouponType getCouponType(String activityCode) {
        if (activityCode.contains(SMS_COUPON_PREFIX)) {
            return CouponType.PLATFORM;
        } else if (activityCode.contains(MERCHANT_COUPON_PREFIX)) {
            return CouponType.MERCHANT;
        } else {
            throw new ApiException(GlobalResCodeConstants.ILLEGAL_REQUEST);
        }
    }

    public static boolean isPlatformCoupon(CouponType couponType) {
        return CouponType.PLATFORM.equals(couponType);
    }

    public static boolean isMerchantCoupon(CouponType couponType) {
        return CouponType.MERCHANT.equals(couponType);
    }

}
