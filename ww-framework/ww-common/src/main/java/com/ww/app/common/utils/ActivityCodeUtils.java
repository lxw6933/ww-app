package com.ww.app.common.utils;

import cn.hutool.core.util.StrUtil;

/**
 * @author ww
 * @create 2025-03-06- 09:26
 * @description:
 */
public class ActivityCodeUtils {

    private ActivityCodeUtils() {}

    private static final String SMS_COUPON_PREFIX = "SC";

    public static String getSmsCouponCode() {
        return StrUtil.join(StrUtil.EMPTY, SMS_COUPON_PREFIX, IdUtil.generatorIdStr());
    }

}
