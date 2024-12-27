package imclient.handler;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.ww.mall.common.utils.MallThreadUtil;
import com.ww.mall.im.common.ImConstant;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ImTestClientStart implements InitializingBean {

    private final ScheduledExecutorService heartbeatExecutor = MallThreadUtil.initScheduledExecutorService(1);

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
                        reConnection(clientGroup.next(), future.cause());
                    }
                });

                Channel channel = channelFuture.channel();
                Scanner scanner = new Scanner(System.in);
                System.out.println("请输入当前用户id：");
                Long userId = scanner.nextLong();
                System.out.println("请输入接收消息用户id：");
                Long objectId = scanner.nextLong();
                // 发送登录消息包
                sendImMsg(userId, ImMsgCodeEnum.IM_LOGIN_MSG, channel);
                // 心跳包机制
                initHeartBeatTask(userId, channel);
                while (true) {
                    System.out.println("请输入聊天内容");
                    String content = scanner.nextLine();
                    if (StrUtil.isEmpty(content)) {
                        continue;
                    }
                    // 发送业务消息
                    ImMsgBody bizBody = new ImMsgBody();
                    bizBody.setAppId(ImAppIdEnum.MALL_LIVE_BIZ.getCode());
                    bizBody.setUserId(objectId);
                    bizBody.setBizCode(ImMsgBizCodeEnum.CHAT_MSG_BIZ.getCode());
                    // 构建业务消息对象
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
                reConnection(clientGroup.next(), e);
            }
        });
        clientThread.start();
    }

    private void sendImMsg(Long userId, ImMsgCodeEnum imLoginMsg, Channel channel) {
        ImMsgBody imMsgBody = new ImMsgBody();
        imMsgBody.setAppId(ImAppIdEnum.MALL_LIVE_BIZ.getCode());
        imMsgBody.setUserId(userId);
        ImMsg loginMsg = ImMsg.build(imLoginMsg.getCode(), JSON.toJSONString(imMsgBody));
        channel.writeAndFlush(loginMsg);
    }

    public void reConnection(EventLoop eventLoop, Throwable e) {
        log.error("连接im server服务失败，三秒后尝试重连...", e);
        eventLoop.schedule(this::start, 3, TimeUnit.SECONDS);
    }

    public void initHeartBeatTask(Long userId, Channel channel) {
        heartbeatExecutor.schedule(() -> sendImMsg(userId, ImMsgCodeEnum.IM_HEARTBEAT_MSG, channel), ImConstant.DEFAULT_HEART_BEAT_GAP, TimeUnit.SECONDS);
    }
}
