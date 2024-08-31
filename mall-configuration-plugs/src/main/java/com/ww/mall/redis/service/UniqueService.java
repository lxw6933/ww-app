package com.ww.mall.redis.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

/**
 * @author ww
 * @create 2024-08-31 13:21
 * @description:
 */
@Slf4j
public class UniqueService {

    private final RedissonClient redissonClient;

    private final RBloomFilter<String> bloomFilter;

    private final long size;

    private final double falseProbability;

    private final int shardNum;

    private static final String BLOOM_FILTER_PREFIX = "bf:unique:";
    private static final String SET_PREFIX= "set:unique:";
    private final String KEY;


    public UniqueService(RedissonClient redissonClient, String bloomFilterKey) {
        this.size = 100000000;
        this.falseProbability = 0.01;
        this.shardNum = (int) (size / 10000);
        this.KEY = bloomFilterKey;
        this.redissonClient = redissonClient;
        // init bloomFilter
        bloomFilter = redissonClient.getBloomFilter(BLOOM_FILTER_PREFIX + KEY);
        if (!bloomFilter.isExists()) {
            bloomFilter.tryInit(size, falseProbability);
        }
    }

    public UniqueService(RedissonClient redissonClient, String bloomFilterKey, long expectedInsertions, double falseProbability) {
        this.size = expectedInsertions;
        this.falseProbability = falseProbability;
        this.shardNum = (int) (size / 10000);
        this.KEY = bloomFilterKey;
        this.redissonClient = redissonClient;
        // init bloomFilter
        bloomFilter = redissonClient.getBloomFilter(BLOOM_FILTER_PREFIX + KEY);
        if (!bloomFilter.isExists()) {
            bloomFilter.tryInit(expectedInsertions, falseProbability);
        }
    }

    /**
     * 获取set分区key
     *
     * @param target 目标字符串
     * @return set分区key
     */
    private String getShardKey(String target) {
        int shardId = Math.abs(target.hashCode()) % shardNum;
        return SET_PREFIX + KEY + shardId;
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
            if (!bloomFilter.contains(target)) {
                // not exists bloomFilter add to bloomFilter
                bloomFilter.add(target);
            }
            // check in Set and add if not exists
            return !outOrderCodeSet.add(target);
        } catch (Exception e) {
            log.error("【{}】校验异常", target, e);
            throw e;
        }
    }

}
