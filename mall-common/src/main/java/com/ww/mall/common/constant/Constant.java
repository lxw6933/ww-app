package com.ww.mall.common.constant;

/**
 * @description: 系统常量
 * @author: ww
 * @create: 2021-05-13 10:37
 */
public class Constant {

    public static final String SPLIT  = ":";
    public static final String GRAY_VERSION = "gray-version";
    public static final String PROD_VERSION = "prod-version";
    public static final String GRAY_TAG = "gray-tag";
    public static final String GRAY_TAG_VALUE = "true";
    public static final String USER_TOKEN = "token";
    public static final String USER_REAL_IP = "user-real-ip";
    public static final String USER_TOKEN_INFO = "tokenInfo";
    public static final String TEMP_USER_KEY = "temp-user-key";
    public static final Integer TEMP_USER_COOKIE_TIMEOUT = 30 * 24 * 60 * 60;

    public static final String TRACE_ID = "traceId";

    public static final String ENCRYPT_HEADER = "mall-encrypt";

    /**
     * 远程调用标识【相应结果不加密】
     */
    public static final String FEIGN_FLAG = "feign_flag";

    /**
     * 参数加密key
     */
    public static final String SECRET_KEY = "ww6933@sina.com.";

    /**
     * app版本更新提示
     */
    public static final String UPDATE_TIP = "当前版本过低，请及时更新最新版本app!";

}
