package com.ww.mall.im.component.key;

import com.ww.mall.redis.key.RedisKeyBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-12-24 13:44
 * @description:
 */
@Slf4j
@Component
public class ImRedisKeyBuilder extends RedisKeyBuilder {

    private static final String IM_ONLINE_ZSET = "im_online_set";
    private static final String IM_ACK_MAP = "im_ack_map";

    private static final int ACK_SHARD_NUM = 100;
    private static final int ONLINE_SHARD_NUM = 10000;

    /**
     * 消息ack key
     */
    public String buildImAckHashKey(Long userId, int appId) {
        return StringUtils.joinWith(SPLIT_ITEM, super.getPrefix(), IM_ACK_MAP, appId, userId % ACK_SHARD_NUM);
    }

    /**
     * 构建im连接用户心跳 key
     */
    public String buildImLoginUserHeartbeatKey(Long userId, int appId) {
        return StringUtils.joinWith(SPLIT_ITEM, super.getPrefix(), IM_ONLINE_ZSET, appId, userId % ONLINE_SHARD_NUM);
    }

}
