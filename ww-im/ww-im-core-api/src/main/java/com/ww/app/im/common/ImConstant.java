package com.ww.app.im.common;

/**
 * @author ww
 * @create 2024-12-24 10:49
 * @description:
 */
public class ImConstant {

    /**
     * 默认消息协议【魔数、版本号、序列化类型】
     */
    public static final int DEFAULT_MAGIC = 6933;
    public static final short DEFAULT_VERSION = 1;
    public static final short DEFAULT_SERIALIZER = 1;

    /**
     * 默认客户端每次心跳请求的间隔，30秒
     */
    public static final int DEFAULT_HEART_BEAT_GAP = 30;

    public static final String IM_BIND_IP_KEY = "im:bindIp";

    public static final String AT = "@";

}
