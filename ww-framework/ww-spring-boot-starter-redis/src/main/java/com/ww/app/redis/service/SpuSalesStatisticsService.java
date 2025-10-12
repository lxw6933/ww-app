package com.ww.app.redis.service;

import com.ww.app.common.utils.ThreadUtil;
import com.ww.app.redis.component.key.SpuRedisKeyBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author ww
 * @create 2024-08-14- 14:50
 * @description: 销量统计service
 */
@Slf4j
@Component
@ConditionalOnBean(RedisTemplate.class)
public class SpuSalesStatisticsService {

    private final static Map<String, AtomicLong> salesMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService salesDataSyncScheduler = Executors.newScheduledThreadPool(1);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SpuRedisKeyBuilder spuRedisKeyBuilder;

    public SpuSalesStatisticsService() {
        log.info("销量统计业务初始化...");
        // 开启定时任务
        salesDataSyncScheduler.scheduleAtFixedRate(this::syncSaleDataToRedis, 0, 1, TimeUnit.HOURS);
        // 新增狗子
        Runtime.getRuntime().addShutdownHook(new Thread(this::syncSaleDataToRedis));
    }

    /**
     * 统计销量数据
     *
     * @param spuId     商品id
     * @param channelId 渠道id
     * @param num       数量【新增：正数  减少：负数】
     */
    public void recordSpuSale(Long spuId, Long channelId, int num) {
        salesMap.computeIfAbsent(spuRedisKeyBuilder.buildSpuMapKey(channelId, spuId), k -> new AtomicLong()).addAndGet(num);
    }

    /**
     * 将本地销量数据同步到redis
     */
    private void syncSaleDataToRedis() {
        salesMap.forEach((key, atomicLong) -> {
            long res = atomicLong.getAndSet(0);
            if (res > 0) {
                log.info("[{}]销量数据同步到redis, 销量[{}]", key, res);
                stringRedisTemplate.opsForValue().increment(spuRedisKeyBuilder.buildSpuSaleKey(key), res);
            }
        });
    }

    @PreDestroy
    public void destroy() {
        ThreadUtil.shutdown("SpuSalesStatisticsService", this::syncSaleDataToRedis, salesDataSyncScheduler);
    }

}
