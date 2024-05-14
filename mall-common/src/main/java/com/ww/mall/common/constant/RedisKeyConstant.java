package com.ww.mall.common.constant;

/**
 * @description: redis key 命名规范：【业务名:数据名:变量】
 * @author: ww
 * @create: 2021-05-17 15:28
 */
public class RedisKeyConstant {

    private RedisKeyConstant() {}

    public static final String SPLIT_KEY = ":";

    public static final String GEO_KEY = "geo:";

    public static final String SMS_CODE_CACHE_PREFIX = "sms:code:";

    public static final String SECKILL_PATH_PREFIX = "mall-seckill:seckill:path:";
    public static final String SECKILL_CODE_PREFIX = "mall-seckill:seckill:captcha:";

}
