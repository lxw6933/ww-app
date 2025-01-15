package com.ww.app.im.handler.msg;

import com.ww.app.im.common.ImMsg;
import com.ww.app.im.core.api.common.ImMsgBody;
import com.ww.app.im.core.api.enums.ImMsgCodeEnum;
import com.ww.app.im.utils.ImChannelHandlerContextUtils;
import com.ww.app.im.utils.ImContextUtils;
import com.ww.app.im.core.api.utils.ImUtils;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-11-09 20:18
 * @description:
 */
@Slf4j
@Component
public class LogoutMsgHandlerAdapter implements ImMsgHandlerAdapter {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void handle(ChannelHandlerContext ctx, ImMsg imMsg) {
        Long userId = ImContextUtils.getUserId(ctx);
        Integer appId = ImContextUtils.getAppId(ctx);
        if (userId == null || appId == null) {
            log.error("用户断开im服务器连接消息异常 {}", imMsg);
            ctx.close();
            throw new IllegalArgumentException("退出异常");
        }
        // 退出业务处理
        logoutHandler(ctx, userId, appId);
    }

    @Override
    public ImMsgCodeEnum getMsgType() {
        return ImMsgCodeEnum.IM_LOGOUT_MSG;
    }

    /**
     * 退出 清理用户相关缓存
     */
    public void logoutHandler(ChannelHandlerContext ctx, Long userId, Integer appId) {
        log.info("用户[{}] appId:[{}]已断开连接", userId, appId);
        // 通知客户端，服务端已接收到退出通知
        ImMsgBody respBody = new ImMsgBody();
        respBody.setAppId(appId);
        respBody.setUserId(userId);
        respBody.setBizMsg("true");
        ImMsg respMsg = ImMsg.build(ImMsgCodeEnum.IM_LOGOUT_MSG.getCode(), respBody);
        ctx.writeAndFlush(respMsg);
        ctx.close();
        // 清除渠道与用户的缓存
        ImChannelHandlerContextUtils.remove(userId);
        // 清除用户与im服务ip的绑定信息
        stringRedisTemplate.delete(ImUtils.buildImBindIpKey(userId, appId));
        // 清除渠道属性与用户的缓存
        ImContextUtils.removeUserId(ctx);
        ImContextUtils.removeAppId(ctx);
        // TODO mq通知
    }

}
