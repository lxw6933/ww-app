package com.ww.mall.im.handler.msg;

import com.alibaba.fastjson.JSON;
import com.ww.mall.im.common.ImConstant;
import com.ww.mall.im.common.ImMsg;
import com.ww.mall.im.common.ImMsgBody;
import com.ww.mall.im.component.key.ImRedisKeyBuilder;
import com.ww.mall.im.enums.ImMsgCodeEnum;
import com.ww.mall.im.utils.ImContextUtils;
import com.ww.mall.im.utils.ImUtils;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
public class HeartBeatMsgHandlerAdapter implements ImMsgHandlerAdapter {

    @Resource
    private ImRedisKeyBuilder imRedisKeyBuilder;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void handle(ChannelHandlerContext ctx, ImMsg imMsg) {
        Long userId = ImContextUtils.getUserId(ctx);
        Integer appId = ImContextUtils.getAppId(ctx);
        if (userId == null || appId == null) {
            log.error("心跳消息异常 {}", imMsg);
            ctx.close();
            throw new IllegalArgumentException("心跳消息异常");
        }
        String heartBeatKey = imRedisKeyBuilder.buildImLoginUserHeartbeatKey(userId, appId);
        // 记录用户心跳信息
        recordHeartbeat(userId, heartBeatKey);
        // 处理用户心跳信息异常信息
        handleExpireRecordHeartbeat(heartBeatKey);
        redisTemplate.expire(heartBeatKey, 5, TimeUnit.MINUTES);
        // 重置用户与im服务器ip绑定时长信息
        stringRedisTemplate.expire(ImUtils.buildImBindIpKey(userId, appId), ImConstant.DEFAULT_HEART_BEAT_GAP * 2, TimeUnit.SECONDS);

        ImMsgBody msgBody = new ImMsgBody();
        msgBody.setUserId(userId);
        msgBody.setAppId(appId);
        msgBody.setBizMsg("true");
        ImMsg respMsg = ImMsg.build(ImMsgCodeEnum.IM_HEARTBEAT_MSG.getCode(), JSON.toJSONString(msgBody));
        log.debug("[heartbeat] imMsg is {}", imMsg);
        ctx.writeAndFlush(respMsg);
    }

    @Override
    public ImMsgCodeEnum getMsgType() {
        return ImMsgCodeEnum.IM_HEARTBEAT_MSG;
    }

    /**
     * 记录用户最近一次心跳时间到zSet上
     */
    private void recordHeartbeat(Long userId, String heartbeatKey) {
        redisTemplate.opsForZSet().add(heartbeatKey, userId, System.currentTimeMillis());
    }

    /**
     * 处理用户不在线留下的心跳记录(在两次心跳包的发送间隔中，如果没有重新更新score值，就会导致被删除)
     */
    private void handleExpireRecordHeartbeat(String redisKey) {
        redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, System.currentTimeMillis() - ImConstant.DEFAULT_HEART_BEAT_GAP * 1000 * 2);
    }

}
