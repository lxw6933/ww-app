package com.ww.app.im.common;

import com.ww.app.common.constant.Constant;
import io.netty.util.AttributeKey;

/**
 * @author ww
 * @create 2024-11-10 0:01
 * @description:
 */
public class ImContextAttr {

    /**
     * traceId
     */
    public static AttributeKey<String> TRACE_ID = AttributeKey.valueOf(Constant.TRACE_ID);

    /**
     * 绑定用户id
     */
    public static AttributeKey<Long> USER_ID = AttributeKey.valueOf("userId");

    /**
     * 绑定appId
     */
    public static AttributeKey<Integer> APP_ID = AttributeKey.valueOf("appId");

    /**
     * 绑定直播间id
     */
    public static AttributeKey<Integer> ROOM_ID = AttributeKey.valueOf("roomId");

}
