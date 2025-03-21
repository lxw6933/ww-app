package com.ww.app.redis.service;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ww
 * @create 2024-10-12- 09:34
 * @description:
 */
@Slf4j
@Component
public class CodeBloomFilterComponent {

    @Resource
    private RedissonClient redissonClient;

    private static final String CODE_BLOOM_FILTER_KEY = "codeBloomFilter:";

    // 存储多个布隆过滤器，每个分片对应一个布隆过滤器
    private static final Map<Integer, RBloomFilter<String>> codeBloomFilterMap = new HashMap<>();

    // 分片数量，比如将数据分成10片，每片处理5000w数据, 容量不影響命令執行時間
    private static final int DEFAULT_SHARD_COUNT = 10;

    @PostConstruct
    public void init() {
        for (int i = 0; i < DEFAULT_SHARD_COUNT; i++) {
            RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(CODE_BLOOM_FILTER_KEY + i);
            if (!bloomFilter.isExists()) {
                bloomFilter.tryInit(5000000L, 0.001);
            }
            codeBloomFilterMap.put(i, bloomFilter);
        }
    }

    /**
     * 根据数据的哈希值，选择合适的分片
     *
     * @param data 数据
     * @return 对应的布隆过滤器分片
     */
    private RBloomFilter<String> getBloomFilterShard(String data) {
        // 根据数据的哈希值选择分片
        int shardIndex = Math.abs(data.hashCode() % DEFAULT_SHARD_COUNT);
        return codeBloomFilterMap.get(shardIndex);
    }

    /**
     * 添加数据
     *
     * @param data 要添加的数据
     */
    public boolean addData(String data) {
        RBloomFilter<String> bloomFilter = getBloomFilterShard(data);
        return bloomFilter.add(data);
    }

    /**
     * 判断数据是否已经存在
     *
     * @param data 数据
     * @return 是否存在
     */
    public boolean containsData(String data) {
        RBloomFilter<String> bloomFilter = getBloomFilterShard(data);
        return bloomFilter.contains(data);
    }

}
