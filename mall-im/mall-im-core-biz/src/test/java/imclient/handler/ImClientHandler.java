package imclient.handler;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ww.mall.im.common.ImMsg;
import com.ww.mall.im.common.ImMsgBody;
import com.ww.mall.im.enums.ImAppIdEnum;
import com.ww.mall.im.enums.ImMsgCodeEnum;
import com.ww.mall.im.handler.codec.ImMsgCodecHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Scanner;

@Service
public class ImClientHandler implements InitializingBean {

    @Resource
    private ImMsgCodecHandler imMsgCodecHandler;

    @Override
    public void afterPropertiesSet() {
        Thread clientThread = new Thread(() -> {
            EventLoopGroup clientGroup = new NioEventLoopGroup();
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(clientGroup);
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    System.out.println("初始化连接建立");
                    ch.pipeline().addLast(imMsgCodecHandler);
                    ch.pipeline().addLast(new ClientHandler());
                }
            });

            ChannelFuture channelFuture;
            try {
                channelFuture = bootstrap.connect("localhost", 8085).sync();
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
                    bizBody.setUserId(userId);
                    bizBody.setBizCode(5555);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("userId", userId);
                    jsonObject.put("objectId", objectId);
                    jsonObject.put("content", content);
                    bizBody.setBizMsg(JSON.toJSONString(jsonObject));
                    ImMsg heartBeatMsg = ImMsg.build(ImMsgCodeEnum.IM_BIZ_MSG.getCode(), JSON.toJSONString(bizBody));
                    channel.writeAndFlush(heartBeatMsg);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
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
