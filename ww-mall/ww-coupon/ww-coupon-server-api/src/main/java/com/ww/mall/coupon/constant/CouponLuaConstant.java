package com.ww.mall.coupon.constant;

/**
 * @author ww
 * @create 2025-03-07 20:14
 * @description:
 */
public class CouponLuaConstant {

    public static final String CONVERT_COUPON_CODE_LUA = "local key = KEYS[1] "
            + "local element = ARGV[1] "
            + "local placeholder = ARGV[2] "
            + "local removed = redis.call('SREM', key, element) "
            + "if removed == 0 then "
            + "    return -1 "
            + "end "
            + "local size = redis.call('SCARD', key) "
            + "if size == 0 then "
            + "    redis.call('SADD', key, placeholder) "
            + "end "
            + "return 1";
    public static final byte[] CONVERT_COUPON_CODE_LUA_BYTE = CONVERT_COUPON_CODE_LUA.getBytes();

}
