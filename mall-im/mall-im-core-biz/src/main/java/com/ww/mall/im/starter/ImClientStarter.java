package com.ww.mall.im.starter;

import com.alibaba.fastjson.JSON;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.im.common.ImMsg;
import com.ww.mall.im.common.ImMsgBody;
import com.ww.mall.im.configuration.ImProperties;
import com.ww.mall.im.enums.ImMsgCodeEnum;
import com.ww.mall.im.handler.initializer.ImClientHandlerInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2024-05-08- 13:52
 * @description:
 */
@Slf4j
@Component
public class ImClientStarter implements InitializingBean {

    private Channel socketChannel;

    private final EventLoopGroup group = new NioEventLoopGroup();

    @Resource
    private ImProperties imProperties;

    @Resource
    private ImClientHandlerInitializer imClientHandlerInitializer;

//    public void sendMsg(ImMsg message) {
//        log.info("客戶端發送消息：{}", message);
//        socketChannel.writeAndFlush(message);
//    }

    public void start() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(imClientHandlerInitializer);
        ChannelFuture future;
        try {
            future = bootstrap.connect("127.0.0.1", imProperties.getPort()).sync();
        } catch (InterruptedException e) {
            log.error("鏈接服務器異常", e);
            throw new ApiException("鏈接im服務器異常");
        }
        // 客户端连接服务端逻辑
        future.addListener((ChannelFutureListener) channelFuture -> {
            if (channelFuture.isSuccess()) {
                log.info("connect netty server success");
            } else {
                log.warn("connect netty server fail, 3s after try to reconnect");
                channelFuture.channel().eventLoop().schedule(this::start, 3, TimeUnit.SECONDS);
            }
        });
        Channel channel = future.channel();
        while (true) {
            log.info("client send msg");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            ImMsgBody msg = new ImMsgBody();
            channel.writeAndFlush(ImMsg.build(ImMsgCodeEnum.IM_LOGIN_MSG.getCode(), JSON.toJSONString(msg)));
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Thread.sleep(1000);
        new Thread(this::start, "im client").start();
    }
}
