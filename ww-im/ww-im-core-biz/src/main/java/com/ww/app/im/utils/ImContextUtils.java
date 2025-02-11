package com.ww.app.im.utils;

import com.ww.app.im.common.ImContextAttr;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author ww
 * @create 2024-11-10 0:00
 * @description:
 */
public class ImContextUtils {

    public static String getTraceId(ChannelHandlerContext ctx) {
        return ctx.channel().attr(ImContextAttr.TRACE_ID).get();
    }

    public static void setTraceId(ChannelHandlerContext ctx, String traceId) {
        ctx.channel().attr(ImContextAttr.TRACE_ID).set(traceId);
    }

    public static Integer getRoomId(ChannelHandlerContext ctx) {
        return ctx.channel().attr(ImContextAttr.ROOM_ID).get();
    }

    public static void setRoomId(ChannelHandlerContext ctx, int roomId) {
        ctx.channel().attr(ImContextAttr.ROOM_ID).set(roomId);
    }

    public static void setAppId(ChannelHandlerContext ctx, int appId) {
        ctx.channel().attr(ImContextAttr.APP_ID).set(appId);
    }

    public static Integer getAppId(ChannelHandlerContext ctx) {
        return ctx.channel().attr(ImContextAttr.APP_ID).get();
    }

    public static void setUserId(ChannelHandlerContext ctx, Long userId) {
        ctx.channel().attr(ImContextAttr.USER_ID).set(userId);
    }

    public static Long getUserId(ChannelHandlerContext ctx) {
        return ctx.channel().attr(ImContextAttr.USER_ID).get();
    }

    public static void removeUserId(ChannelHandlerContext ctx) {
        ctx.channel().attr(ImContextAttr.USER_ID).set(null);
    }

    public static void removeAppId(ChannelHandlerContext ctx) {
        ctx.channel().attr(ImContextAttr.APP_ID).set(null);
    }

    public static void removeTraceId(ChannelHandlerContext ctx) {
        ctx.channel().attr(ImContextAttr.TRACE_ID).set(null);
    }

}
