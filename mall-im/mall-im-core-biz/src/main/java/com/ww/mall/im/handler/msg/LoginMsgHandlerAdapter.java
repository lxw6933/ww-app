package com.ww.mall.im.handler.msg;

import cn.hutool.core.lang.Assert;
import com.alibaba.fastjson.JSON;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.im.common.ImConstant;
import com.ww.mall.im.common.ImMsg;
import com.ww.mall.im.common.ImMsgBody;
import com.ww.mall.im.component.key.ImRedisKeyBuilder;
import com.ww.mall.im.enums.ImMsgCodeEnum;
import com.ww.mall.im.handler.component.ImHandlerComponent;
import com.ww.mall.im.utils.ImChannelHandlerContextUtils;
import com.ww.mall.im.utils.ImContextUtils;
import com.ww.mall.im.utils.ImUtils;
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
    private ImHandlerComponent imHandlerComponent;

    @Resource
    private ImRedisKeyBuilder imRedisKeyBuilder;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void handle(ChannelHandlerContext ctx, ImMsg imMsg) {
        Assert.isTrue(imMsg.validData(), () -> new IllegalArgumentException("body error"));
        ImMsgBody imMsgBody = imHandlerComponent.deserializeMsg(imMsg);
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
        ImMsg respMsg = ImMsg.build(ImMsgCodeEnum.IM_LOGIN_MSG.getCode(), JSON.toJSONString(respBody));
        ctx.writeAndFlush(respMsg);
        // TODO 广播用户登录消息

    }

    @Override
    public ImMsgCodeEnum getMsgType() {
        return ImMsgCodeEnum.IM_LOGIN_MSG;
    }
}
