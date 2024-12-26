package imclient.handler;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.ww.mall.im.common.ImMsg;
import com.ww.mall.im.common.ImMsgBody;
import com.ww.mall.im.dto.MessageDTO;
import com.ww.mall.im.enums.ImAppIdEnum;
import com.ww.mall.im.enums.ImMsgBizCodeEnum;
import com.ww.mall.im.enums.ImMsgCodeEnum;
import com.ww.mall.im.handler.codec.ImMsgCodecHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ImTestClientStart implements InitializingBean {

    @Override
    public void afterPropertiesSet() {
        start();
    }

    public void start() {
        Thread clientThread = new Thread(() -> {
            EventLoopGroup clientGroup = new NioEventLoopGroup();
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(clientGroup);
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    System.out.println("初始化连接建立");
                    ch.pipeline().addLast(new ImMsgCodecHandler());
                    ch.pipeline().addLast(new ClientHandler());
                }
            });

            ChannelFuture channelFuture = bootstrap.connect("localhost", 8765);
            try {
                log.info("开始连接im server");
                channelFuture.sync();

                channelFuture.addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        log.info("成功连接im server");
                    } else {
                        log.error("连接im server服务失败，三秒后尝试重连...");
                        future.channel().eventLoop().schedule(this::start, 3, TimeUnit.SECONDS);
                    }
                });

                Channel channel = channelFuture.channel();
                Scanner scanner = new Scanner(System.in);
                System.out.println("请输入userId");
                Long userId = scanner.nextLong();
                System.out.println("请输入objectId");
                Long objectId = scanner.nextLong();
                //发送登录消息包
                ImMsgBody imMsgBody = new ImMsgBody();
                imMsgBody.setAppId(ImAppIdEnum.MALL_LIVE_BIZ.getCode());
                imMsgBody.setUserId(userId);
                ImMsg loginMsg = ImMsg.build(ImMsgCodeEnum.IM_LOGIN_MSG.getCode(), JSON.toJSONString(imMsgBody));
                channel.writeAndFlush(loginMsg);
                //心跳包机制
                sendHeartBeat(userId, channel);
                while (true) {
                    System.out.println("请输入聊天内容");
                    String content = scanner.nextLine();
                    if (StrUtil.isEmpty(content)) {
                        continue;
                    }
                    ImMsgBody bizBody = new ImMsgBody();
                    bizBody.setAppId(ImAppIdEnum.MALL_LIVE_BIZ.getCode());
                    bizBody.setUserId(objectId);
                    bizBody.setBizCode(ImMsgBizCodeEnum.CHAT_MSG_BIZ.getCode());
                    MessageDTO message = new MessageDTO();
                    message.setUserId(userId);
                    message.setRoomId(null);
                    message.setContent(content);
                    message.setCreateTime(new Date());
                    bizBody.setBizMsg(JSON.toJSONString(message));
                    ImMsg heartBeatMsg = ImMsg.build(ImMsgCodeEnum.IM_BIZ_MSG.getCode(), JSON.toJSONString(bizBody));
                    channel.writeAndFlush(heartBeatMsg);
                }
            } catch (Exception e) {
                log.error("连接im server服务失败，三秒后尝试重连...", e);
                clientGroup.schedule(this::start, 3, TimeUnit.SECONDS);
            }
        });
        clientThread.start();
    }


    private void sendHeartBeat(Long userId, Channel channel) {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ImMsgBody imMsgBody = new ImMsgBody();
                imMsgBody.setAppId(ImAppIdEnum.MALL_LIVE_BIZ.getCode());
                imMsgBody.setUserId(userId);
                ImMsg loginMsg = ImMsg.build(ImMsgCodeEnum.IM_HEARTBEAT_MSG.getCode(), JSON.toJSONString(imMsgBody));
                channel.writeAndFlush(loginMsg);
            }
        }).start();
    }
}
