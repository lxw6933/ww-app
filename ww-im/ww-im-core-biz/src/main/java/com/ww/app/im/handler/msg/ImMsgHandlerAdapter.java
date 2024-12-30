package com.ww.app.im.handler.msg;

import com.ww.app.im.common.ImMsg;
import com.ww.app.im.enums.ImMsgCodeEnum;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author ww
 * @create 2024-11-09 19:27
 * @description:
 */
public interface ImMsgHandlerAdapter {

    /**
     * 消息处理工厂
     *
     * @param ctx ctx
     * @param imMsg imMsg
     */
    void handle(ChannelHandlerContext ctx, ImMsg imMsg);

//    /**
//     * 消息适配
//     *
//     * @param msgType 消息类型
//     * @return boolean
//     */
//    default boolean support(int msgType) {
//        return getMsgType().getCode() == msgType;
//    }

    /**
     * 消息code
     */
    ImMsgCodeEnum getMsgType();

}
