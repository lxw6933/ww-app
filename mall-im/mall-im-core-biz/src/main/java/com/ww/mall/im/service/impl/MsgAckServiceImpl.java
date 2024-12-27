package com.ww.mall.im.service.impl;

import com.ww.mall.im.common.ImMsgBody;
import com.ww.mall.im.component.key.ImRedisKeyBuilder;
import com.ww.mall.im.enums.ImMsgCodeEnum;
import com.ww.mall.im.service.MsgAckService;
import com.ww.mall.im.service.MsgRouterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2024-12-24 16:52
 * @description:
 */
@Slf4j
@Service
public class MsgAckServiceImpl implements MsgAckService {

    private static final int ACK_KEY_EXPIRE = 30;

    @Resource
    private ImRedisKeyBuilder imRedisKeyBuilder;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private MsgRouterService msgRouterService;

    @Override
    public void doMsgAck(ImMsgBody imMsgBody) {
        String ackKey = imRedisKeyBuilder.buildImAckHashKey(imMsgBody.getUserId(), imMsgBody.getAppId());
        redisTemplate.opsForHash().delete(ackKey, imMsgBody.getSeqId());
        redisTemplate.expire(ackKey, ACK_KEY_EXPIRE, TimeUnit.MINUTES);
        // 通知发送方消息已接收
        log.info("消息确认：{}", imMsgBody);
        msgRouterService.sendMsgToClient(ImMsgCodeEnum.IM_ACK_MSG, imMsgBody);
    }

    @Override
    public void recordMsgAck(ImMsgBody imMsgBody, int times) {
        String ackKey = imRedisKeyBuilder.buildImAckHashKey(imMsgBody.getUserId(), imMsgBody.getAppId());
        redisTemplate.opsForHash().put(ackKey, imMsgBody.getSeqId(), times);
        redisTemplate.expire(ackKey, ACK_KEY_EXPIRE, TimeUnit.MINUTES);
    }

    @Override
    public void sendDelayMsg(ImMsgBody imMsgBody) {

    }

    @Override
    public int getMsgAckTimes(String msgId, long userId, int appId) {
        String ackKey = imRedisKeyBuilder.buildImAckHashKey(userId, appId);
        Object value = redisTemplate.opsForHash().get(ackKey, msgId);
        if (value == null) {
            return -1;
        }
        return (int) value;
    }
}
