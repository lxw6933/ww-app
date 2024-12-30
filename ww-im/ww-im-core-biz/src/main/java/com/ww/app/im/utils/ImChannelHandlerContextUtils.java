package com.ww.app.im.utils;

import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ww
 * @create 2024-11-09 23:56
 * @description:
 */
public class ImChannelHandlerContextUtils {

    /**
     * 当前im服务器 ip
     */
    private static String IM_SERVER_IP = "";

    /**
     * im server 用户 channel 关联缓存
     */
    private static final Map<Long, ChannelHandlerContext> channelHandlerContextMap = new ConcurrentHashMap<>(16);

    public static String getImServerIp() {
        return IM_SERVER_IP;
    }

    public static void setImServerIp(String serverIpAddress) {
        IM_SERVER_IP = serverIpAddress;
    }

    public static ChannelHandlerContext get(Long userId) {
        return channelHandlerContextMap.get(userId);
    }

    public static void set(Long userId, ChannelHandlerContext channelHandlerContext) {
        channelHandlerContextMap.put(userId, channelHandlerContext);
    }

    public static void remove(Long userId) {
        channelHandlerContextMap.remove(userId);
    }
}
