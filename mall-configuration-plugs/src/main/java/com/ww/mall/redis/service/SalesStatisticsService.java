package com.ww.mall.redis.service;

import com.ww.mall.common.constant.Constant;
import com.ww.mall.common.constant.RedisKeyConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author ww
 * @create 2024-08-14- 14:50
 * @description: 销量统计service
 */
@Slf4j
@Component
@ConditionalOnBean(RedisTemplate.class)
public class SalesStatisticsService implements DisposableBean {

    private final static Map<String, LongAdder> salesMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService salesDataSyncScheduler = Executors.newScheduledThreadPool(1);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public SalesStatisticsService() {
        log.info("销量统计业务初始化...");
        // 开启定时任务
        salesDataSyncScheduler.scheduleAtFixedRate(this::syncSaleDataToRedis, 0, 1, TimeUnit.HOURS);
        // 新增狗子
        Runtime.getRuntime().addShutdownHook(new Thread(this::syncSaleDataToRedis));
    }

    /**
     * 统计销量数据
     *
     * @param spuId 商品id
     * @param channelId 渠道id
     * @param num 数量【新增：正数  减少：负数】
     */
    public void recordSpuSale(Long spuId, Long channelId, int num) {
        salesMap.computeIfAbsent(StringUtils.joinWith(Constant.SPLIT, channelId, spuId), k -> new LongAdder()).add(num);
    }

    /**
     * 将本地销量数据同步到redis
     */
    private void syncSaleDataToRedis() {
        salesMap.forEach((key, longAddr) -> {
            if (longAddr.sum() > 0) {
                log.info("【{}】销量数据同步到redis, 销量【{}】", key, longAddr.sum());
                redisTemplate.opsForValue().increment(RedisKeyConstant.SPU_SALE_DATA_PREFIX + key, longAddr.sumThenReset());
            }
        });
    }

    @Override
    public void destroy() {
        salesDataSyncScheduler.shutdown();
        syncSaleDataToRedis();
    }

}
