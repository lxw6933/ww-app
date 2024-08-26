package com.ww.mall.redis.service;

import cn.hutool.core.collection.CollectionUtil;
import com.ww.mall.common.constant.RedisKeyConstant;
import com.ww.mall.redis.vo.SpuScore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2024-08-14- 14:50
 * @description: 评价信用统计service
 */
@Slf4j
@Component
@ConditionalOnBean(RedissonClient.class)
public class SpuCreditScoreStatisticsService {

    private final static Map<String, CreditScore> creditScoreMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService commentDataSyncScheduler = Executors.newScheduledThreadPool(1);

    @Autowired
    private RedissonClient redissonClient;

    public SpuCreditScoreStatisticsService() {
        log.info("评价信用统计业务初始化...");
        // 开启定时任务
        commentDataSyncScheduler.scheduleAtFixedRate(this::syncCommentDataToRedis, 0, 1, TimeUnit.HOURS);
        // 新增狗子
        Runtime.getRuntime().addShutdownHook(new Thread(this::syncCommentDataToRedis));
    }

    /**
     * 统计评价信用数据
     *
     * @param spuId     商品id
     * @param channelId 渠道id
     * @param score     信用分
     */
    public void recordSpuCreditScore(Long spuId, Long channelId, double score) {
        if (spuId == null || channelId == null) {
            return;
        }
        log.info("本地缓存渠道【{}】商品【{}】评分信用分【{}】", spuId, channelId, score);
        String spuKey = StringUtils.joinWith(RedisKeyConstant.SPLIT_KEY, channelId, spuId);
        creditScoreMap.computeIfAbsent(spuKey, k -> new CreditScore(score)).addScore(score);
    }

    /**
     * 将本地评价信用数据同步到redis
     */
    public void syncCommentDataToRedis() {
        creditScoreMap.forEach((localKey, creditScore) -> {
            List<Object> dataList = creditScore.reset();
            int localCount = (int) dataList.get(0);
            if (localCount > 0) {
                double localTotalScore = (double) dataList.get(1);
                String[] split = localKey.split(RedisKeyConstant.SPLIT_KEY);
                String channelIdStr = split[0];
                String spuIdStr = split[1];
                String hashKey = RedisKeyConstant.SPU_CREDIT_SCORE + channelIdStr;
                log.info("【{}】评价信用数据同步到redis, 评价信用【{}】【{}】", localKey, localCount, localTotalScore);
                RMap<String, SpuScore> spuScoreMap = redissonClient.getMap(hashKey);
                spuScoreMap.compute(spuIdStr, (spuId, spuScore) -> {
                    if (spuScore == null) {
                        spuScore = new SpuScore(localCount, localTotalScore);
                        return spuScore;
                    }
                    spuScore.addScore(localCount, localTotalScore);
                    return spuScore;
                });
                log.info("【{}】评价信用数据同步到redis结果：【{}】", localKey, spuScoreMap.get(spuIdStr));
            }
        });
    }

    @PreDestroy
    public void destroy() {
        commentDataSyncScheduler.shutdown();
        syncCommentDataToRedis();
    }

    @Data
    private static class CreditScore {
        int count;
        double totalScore;

        public CreditScore(double score) {
            this.count = 1;
            this.totalScore = score;
        }

        public CreditScore(int num, double score) {
            this.count = num;
            this.totalScore = score;
        }

        synchronized void addScore(double score) {
            this.count++;
            this.totalScore = this.totalScore + score;
        }

        synchronized List<Object> reset() {
            int count = this.count;
            double totalScore = this.totalScore;
            this.count = 0;
            this.totalScore = 0;
            return CollectionUtil.toList(count, totalScore);
        }

    }

}
