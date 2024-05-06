package com.ww.mall.netty.handler;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson.JSON;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.netty.entity.WSDataContent;
import com.ww.mall.netty.enums.WSMsgAction;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ww
 * @create 2024-05-06- 16:43
 * @description: netty websocket handler
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class MallWSHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    /**
     * 管理所有客户端的channel通道
     */
    public static ChannelGroup clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 用户id和channelId绑定
     */
    private static final Map<String, Channel> managerChannel = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        // 接收到客户端发送的消息
        String content = msg.text();
        Channel currentChannel = ctx.channel();
        try {
            WSDataContent<?> WSDataContent = JSON.parseObject(content, WSDataContent.class);
            if (WSDataContent == null) {
                throw new ApiException("数据异常");
            }
            Integer action = WSDataContent.getAction();
            String msgId = WSDataContent.getMsgId();
            switch (action) {
                case WSMsgAction.CONNECT.type:
                    String uid = WSDataContent.getUid();
                    if (StringUtils.isEmpty(uid)) {
                        // 主动断开连接
                        writeAndFlushResponse(WSMsgAction.BREAK_OFF.type, msgId, null, currentChannel);
                        return;
                    }
                    String loginLabel = WSDataContent.getLoginLabel();
                    Channel existChannel = managerChannel.get(uid);
                    if (existChannel != null) {
                        //存在当前用户的连接，验证登录标签
                        LinkUserService linkUserService = (LinkUserService) SpringUtil.getBean("linkUserServiceImpl");
                        if (linkUserService.checkUserLoginLabel(uid, loginLabel)) {
                            //是同一次登录标签,加入新连接，关闭旧的连接
                            managerChannel.put(uid, currentChannel);
                            writeAndFlushResponse(WSMsgAction.BREAK_OFF.type, null, createKickMsgBody(), existChannel);
                            writeAndFlushResponse(WSMsgAction.MESSAGE_SIGN.type, msgId, null, currentChannel);
                            //existChannel.close();
                        } else {
                            //不是同一次登录标签，拒绝连接
                            writeAndFlushResponse(WSMsgAction.BREAK_OFF.type, null, createKickMsgBody(), currentChannel);
                            //currentChannel.close();
                        }
                    } else {
                        managerChannel.put(uid, currentChannel);
                        writeAndFlushResponse(WSMsgAction.MESSAGE_SIGN.type, msgId, null, currentChannel);
                    }
                    break;
                case WSMsgAction.KEEPALIVE.type:
                    //心跳类型的消息
                    log.info("收到来自Channel为{}的心跳包......", currentChannel);
                    writeAndFlushResponse(WSMsgAction.MESSAGE_SIGN.type, msgId, null, currentChannel);
                    break;
                default:
                    throw new IllegalStateException("非法数据");
            }
        } catch (Exception e) {

        }
        log.info("Received client message: {}", content);
        // 假设这里有一个业务逻辑处理过程
        String responseMessage = "server processed: " + content;
        // 发送消息给客户端
        ctx.channel().writeAndFlush(new TextWebSocketFrame(responseMessage));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        clients.add(ctx.channel());
        log.info("客户端建立连接，通道开启！id={},localAddress={},remoteAddress={}", ctx.channel().id(), ctx.channel().localAddress(), ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        clients.remove(ctx.channel());
        remove(ctx.channel());
        log.info("客户端断开连接，通道关闭！id={},localAddress={},remoteAddress={}", ctx.channel().id(), ctx.channel().localAddress(), ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 异常处理
        Channel channel = ctx.channel();
        ctx.close();
        clients.remove(channel);
        remove(channel);
        log.error("客户端异常，通道关闭！id={},localAddress={},remoteAddress={}", channel.id(), channel.localAddress(), channel.remoteAddress());
    }

    /**
     * 响应客户端
     */
    public static void writeAndFlushResponse(Integer action, String msgId, T data, Channel channel) {
        WSDataContent<T> wsDataContent = new WSDataContent<>();
        wsDataContent.setAction(action);
        wsDataContent.setMsgId(msgId);
        wsDataContent.setData(data);
        channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(wsDataContent)));
    }

    /**
     * 移除Channel
     */
    public static void remove(Channel channel) {
        for (Map.Entry<String, Channel> entry : managerChannel.entrySet()) {
            if (entry.getValue().equals(channel)) {
                managerChannel.remove(entry.getKey());
            }
        }
    }

}
