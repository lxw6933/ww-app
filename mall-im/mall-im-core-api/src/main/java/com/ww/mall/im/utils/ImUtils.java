package com.ww.mall.im.utils;

import cn.hutool.core.util.StrUtil;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.im.common.ImConstant;

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

}
