package imclient.handler;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.ww.app.common.utils.ThreadUtil;
import com.ww.app.im.common.ImConstant;
import com.ww.app.im.common.ImMsg;
import com.ww.app.im.common.ImMsgBody;
import com.ww.app.im.dto.MessageDTO;
import com.ww.app.im.enums.ImAppIdEnum;
import com.ww.app.im.enums.ImMsgBizCodeEnum;
import com.ww.app.im.enums.ImMsgCodeEnum;
import com.ww.app.im.handler.codec.ImMsgCodecHandler;
import com.ww.app.im.protocol.ImProtocolFrameDecoder;
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
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ImTestClientStart implements InitializingBean {

    private final ScheduledExecutorService heartbeatExecutor = ThreadUtil.initScheduledExecutorService(1);

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
                    // 自定义协议
                    ch.pipeline().addLast(new ImProtocolFrameDecoder());
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
                System.out.println("请输入聊天内容");
                while (true) {
                    String content = scanner.nextLine();
                    if (StrUtil.isEmpty(content)) {
                        continue;
                    }
                    // 发送业务消息
                    ImMsgBody bizBody = new ImMsgBody();
                    bizBody.setAppId(ImAppIdEnum.LIVE_BIZ.getCode());
                    bizBody.setUserId(objectId);
                    bizBody.setBizCode(ImMsgBizCodeEnum.CHAT_MSG_BIZ.getCode());
                    bizBody.setSeqId(UUID.randomUUID().toString());
                    // 构建业务消息对象
                    MessageDTO message = new MessageDTO();
                    message.setUserId(userId);
                    message.setRoomId(null);
                    message.setContent(content);
                    message.setCreateTime(new Date());
                    bizBody.setBizMsg(JSON.toJSONString(message));
                    ImMsg bizMsg = ImMsg.buildTestClient(ImMsgCodeEnum.IM_BIZ_MSG.getCode(), ImConstant.DEFAULT_SERIALIZER, JSON.toJSONBytes(bizBody));
                    channel.writeAndFlush(bizMsg);
                    System.out.println("[" + bizBody.getSeqId() + "]消息发送");
                }
            } catch (Exception e) {
                reConnection(clientGroup.next(), e);
            }
        });
        clientThread.start();
    }

    public static void sendImMsg(Long userId, ImMsgCodeEnum imLoginMsg, Channel channel) {
        ImMsgBody imMsgBody = new ImMsgBody();
        imMsgBody.setAppId(ImAppIdEnum.LIVE_BIZ.getCode());
        imMsgBody.setUserId(userId);
        ImMsg loginMsg = ImMsg.buildTestClient(imLoginMsg.getCode(), ImConstant.DEFAULT_SERIALIZER, JSON.toJSONBytes(imMsgBody));
        channel.writeAndFlush(loginMsg);
    }

    public void reConnection(EventLoop eventLoop, Throwable e) {
        log.error("连接im server服务失败，三秒后尝试重连...", e);
        eventLoop.schedule(this::start, 3, TimeUnit.SECONDS);
    }

    public void initHeartBeatTask(Long userId, Channel channel) {
        heartbeatExecutor.scheduleAtFixedRate(() -> sendImMsg(userId, ImMsgCodeEnum.IM_HEARTBEAT_MSG, channel), 1, ImConstant.DEFAULT_HEART_BEAT_GAP, TimeUnit.SECONDS);
    }
}
