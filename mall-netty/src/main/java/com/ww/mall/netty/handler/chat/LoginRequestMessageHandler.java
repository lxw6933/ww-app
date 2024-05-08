package com.ww.mall.netty.handler.chat;

import com.ww.mall.netty.message.chat.req.LoginRequestMessage;
import com.ww.mall.netty.message.chat.res.LoginResponseMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-05-07 21:59
 * @description:
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class LoginRequestMessageHandler extends MallAbstractChatInboundHandler<LoginRequestMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginRequestMessage msg) throws Exception {
        log.info("用户登录请求：{}", msg);
        String username = msg.getUsername();
        String password = msg.getPassword();
        boolean login = userService.login(username, password);
        LoginResponseMessage message;
        if (login) {
            sessionService.bind(ctx.channel(), username);
            message = new LoginResponseMessage(true, "登录成功");
        } else {
            message = new LoginResponseMessage(false, "用户名或密码不正确");
        }
        ctx.writeAndFlush(message);
    }
}
