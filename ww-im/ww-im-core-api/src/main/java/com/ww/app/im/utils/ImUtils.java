package com.ww.app.im.utils;

import cn.hutool.core.util.StrUtil;
import com.ww.app.common.constant.Constant;
import com.ww.app.im.common.ImConstant;
import com.ww.app.im.common.ImMsgBody;
import com.ww.app.proto.im.ImMsgBodyListRequest;
import com.ww.app.proto.im.ImMsgBodyRequest;

import java.util.List;

/**
 * @author ww
 * @create 2024-12-24 22:04
 * @description:
 */
public class ImUtils {

    public static String buildImBindIpKey(Long userId, Integer appId) {
        return StrUtil.join(Constant.SPLIT, ImConstant.IM_BIND_IP_KEY, appId, userId);
    }

    public static String buildImBindIpCache(String imServerIp, Long userId) {
        return StrUtil.join(ImConstant.AT, imServerIp, userId);
    }

    public static String getImBindIp(String imBindIpCache) {
        return StrUtil.subBefore(imBindIpCache, ImConstant.AT, false);
    }

    public static String getImBindUserId(String imBindIpCache) {
        return StrUtil.subAfter(imBindIpCache, ImConstant.AT, false);
    }

    public static ImMsgBodyRequest buildImMsgBodyRequest(ImMsgBody imMsgBody) {
        ImMsgBodyRequest.Builder builder = ImMsgBodyRequest.newBuilder();
        builder.setSeqId(imMsgBody.getSeqId());
        builder.setAppId(imMsgBody.getAppId());
        builder.setUserId(imMsgBody.getUserId());
        builder.setToken(imMsgBody.getToken());
        builder.setBizCode(imMsgBody.getBizCode());
        builder.setBizMsg(imMsgBody.getBizMsg());
        return builder.build();
    }

    public static ImMsgBodyListRequest buildImMsgBodyListRequest(List<ImMsgBody> imMsgBodyList) {
        ImMsgBodyListRequest.Builder builder = ImMsgBodyListRequest.newBuilder();
        imMsgBodyList.forEach(imMsgBody -> {
            ImMsgBodyRequest imMsgBodyRequest = buildImMsgBodyRequest(imMsgBody);
            builder.addImMsgBodyList(imMsgBodyRequest);
        });
        return builder.build();
    }

}
