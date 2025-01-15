package com.ww.app.im.handler.msg;

import cn.hutool.core.lang.Assert;
import com.ww.app.common.exception.ApiException;
import com.ww.app.im.core.api.common.ImConstant;
import com.ww.app.im.common.ImMsg;
import com.ww.app.im.core.api.common.ImMsgBody;
import com.ww.app.im.component.ImMsgSerializerComponent;
import com.ww.app.im.core.api.enums.ImMsgCodeEnum;
import com.ww.app.im.utils.ImChannelHandlerContextUtils;
import com.ww.app.im.utils.ImContextUtils;
import com.ww.app.im.core.api.utils.ImUtils;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2024-11-09 20:18
 * @description:
 */
@Slf4j
@Component
public class LoginMsgHandlerAdapter implements ImMsgHandlerAdapter {

    @Resource
    private ImMsgSerializerComponent imMsgSerializerComponent;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void handle(ChannelHandlerContext ctx, ImMsg imMsg) {
        Assert.isTrue(imMsg.validData(), () -> new IllegalArgumentException("body error"));
        ImMsgBody imMsgBody = imMsgSerializerComponent.deserializeMsg(imMsg);
        log.info("收到客户端发送的登录消息: {}", imMsgBody);
        Long userId = imMsgBody.getUserId();
        int appId = imMsgBody.getAppId();
        if (userId == null || appId == 0) {
            ctx.close();
            throw new ApiException("用户信息异常");
        }
        ImChannelHandlerContextUtils.set(userId, ctx);
        ImContextUtils.setUserId(ctx, userId);
        ImContextUtils.setAppId(ctx, appId);
        // 记录当前用户连接的server ip
        stringRedisTemplate.opsForValue().set(ImUtils.buildImBindIpKey(userId, appId), ImUtils.buildImBindIpCache(ImChannelHandlerContextUtils.getImServerIp(), userId),ImConstant.DEFAULT_HEART_BEAT_GAP * 2, TimeUnit.SECONDS);
        // 给client端反馈imMsg消息
        ImMsgBody respBody = new ImMsgBody();
        respBody.setAppId(appId);
        respBody.setUserId(userId);
        respBody.setBizMsg("true");
        ImMsg respMsg = ImMsg.build(ImMsgCodeEnum.IM_LOGIN_MSG.getCode(), respBody);
        ctx.writeAndFlush(respMsg);
        // TODO 广播用户登录消息

    }

    @Override
    public ImMsgCodeEnum getMsgType() {
        return ImMsgCodeEnum.IM_LOGIN_MSG;
    }
}
