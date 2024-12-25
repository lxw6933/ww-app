package com.ww.mall.im.service.impl;

import com.ww.mall.im.common.ImMsgBody;
import com.ww.mall.im.component.key.ImRedisKeyBuilder;
import com.ww.mall.im.service.MsgAckService;
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

    @Resource
    private ImRedisKeyBuilder imRedisKeyBuilder;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void doMsgAck(ImMsgBody imMsgBody) {
        String ackKey = imRedisKeyBuilder.buildImAckHashKey(imMsgBody.getUserId(), imMsgBody.getAppId());
        redisTemplate.opsForHash().delete(ackKey, imMsgBody.getSeqId());
        redisTemplate.expire(ackKey,30, TimeUnit.MINUTES);
    }

    @Override
    public void recordMsgAck(ImMsgBody imMsgBody, int times) {
        String key = imRedisKeyBuilder.buildImAckHashKey(imMsgBody.getUserId(), imMsgBody.getAppId());
        redisTemplate.opsForHash().put(key, imMsgBody.getSeqId(), times);
        redisTemplate.expire(key,30, TimeUnit.MINUTES);
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
