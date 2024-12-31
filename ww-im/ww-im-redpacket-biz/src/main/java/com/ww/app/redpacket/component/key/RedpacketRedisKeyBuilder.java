package com.ww.app.redpacket.component.key;

import cn.hutool.core.util.StrUtil;
import com.ww.app.redis.key.RedisKeyBuilder;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-12-30- 14:02
 * @description:
 */
@Component
public class RedpacketRedisKeyBuilder extends RedisKeyBuilder {

    private static final String RED_PACKET_KEY = "redpacket";

    public String buildRedpacketKey(String redPacketCode) {
        return super.getPrefix() + StrUtil.join(SPLIT_ITEM, RED_PACKET_KEY, redPacketCode);
    }

}
