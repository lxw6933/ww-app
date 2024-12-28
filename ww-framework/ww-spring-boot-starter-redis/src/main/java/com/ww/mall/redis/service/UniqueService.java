package com.ww.mall.redis.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

/**
 * @author ww
 * @create 2024-08-31 13:21
 * @description:
 */
@Slf4j
public class UniqueService {

    private final String KEY;

    private final int shardNum;

    private final RedissonClient redissonClient;

    public UniqueService(RedissonClient redissonClient, String uniqueKey) {
        this.KEY = uniqueKey;
        this.shardNum = 10000;
        this.redissonClient = redissonClient;
    }

    public UniqueService(RedissonClient redissonClient, String uniqueKey, int initSize) {
        this.KEY = uniqueKey;
        this.shardNum = initSize / 10000;
        this.redissonClient = redissonClient;
    }

    /**
     * 获取set分区key
     *
     * @param target 目标字符串
     * @return set分区key
     */
    private String getShardKey(String target) {
        int shardId = Math.abs(target.hashCode()) % shardNum;
        return KEY + shardId;
    }

    /**
     * 外部订单号是否重复
     *
     * @param target 目标对象
     * @return boolean
     */
    public boolean checkOutOrderCode(String target) {
        if (StringUtils.isEmpty(target)) {
            throw new IllegalArgumentException("外部订单号不能为空");
        }
        if (target.length() > 64) {
            throw new IllegalArgumentException("外部订单号长度不能超过64");
        }
        try {
            // get outOrderCode set shard key
            String shardKey = this.getShardKey(target);
            RSet<String> outOrderCodeSet = redissonClient.getSet(shardKey);
            // check in Set and add if not exists
            return !outOrderCodeSet.add(target);
        } catch (Exception e) {
            log.error("[{}]校验异常", target, e);
            throw e;
        }
    }

    /**
     * 回滚target
     *
     * @param target 目标对象
     * @return boolean
     */
    public boolean removeTargetFormSet(String target) {
        // get outOrderCode set shard key
        String shardKey = this.getShardKey(target);
        RSet<String> outOrderCodeSet = redissonClient.getSet(shardKey);
        // check in Set and add if not exists
        return outOrderCodeSet.remove(target);
    }

}
