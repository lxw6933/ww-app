package com.ww.mall.common.constant;

/**
 * @description: 系统常量
 * @author: ww
 * @create: 2021-05-13 10:37
 */
public class Constant {

    public static final String MYSQL_PRIMARY_KEY = "id";
    public static final String MONGO_PRIMARY_KEY = "_id";

    public static final String UTF_8 = "UTF-8";

    public static final String AES_ECB = "AES/ECB/PKCS5Padding";
    public static final String AES_CBC = "AES/CBC/PKCS5Padding";
    public static final String AES_CFB = "AES/CFB/PKCS5Padding";

    public static final String RSA_PUBLIC_KEY = "publicKey";
    public static final String RSA_PRIVATE_KEY = "privateKey";
    public static final String RSA = "RSA";
    public static final String RSA_SIGNATURE_ALGORITHMS = "SHA256withRSA";

    public static final Long SUPER_ADMIN_MANAGER_ID = 1L;

    public static final String SPLIT  = ":";
    public static final String UNDER_LINE_SPLIT  = "_";
    public static final String GRAY_VERSION = "gray-version";
    public static final String PROD_VERSION = "prod-version";
    public static final String GRAY_TAG = "gray-tag";
    public static final String GRAY_TAG_VALUE = "true";
    public static final String USER_TOKEN_KEY = "Authorization";
    public static final String USER_REAL_IP = "user-real-ip";
    public static final String USER_TOKEN_INFO = "tokenInfo";
    public static final String USER_TYPE = "ww-user-type";
    public static final String TEMP_USER_KEY = "temp-user-key";
    public static final Integer TEMP_USER_COOKIE_TIMEOUT = 30 * 24 * 60 * 60;

    public static final String TRACE_ID = "traceId";
    public static final String MSG_MODE= "msgMode";

    public static final String ENCRYPT_HEADER = "ww-encrypt";

    /**
     * 远程调用标识【相应结果不加密】
     */
    public static final String FEIGN_FLAG = "feign_flag";
    public static final String SERVER_IP = "SERVER_IP";

    /**
     * 参数加密key
     */
    public static final String SECRET_KEY = "ww6933@sina.com.";

    public static final String GATEWAY_REQUEST_FLAG = "ww-mall-gateway";
    public static final String GATEWAY_REQUEST_FLAG_VALUE = "true";

    /**
     * app版本更新提示
     */
    public static final String UPDATE_TIP = "当前版本过低，请及时更新最新版本app!";

}
