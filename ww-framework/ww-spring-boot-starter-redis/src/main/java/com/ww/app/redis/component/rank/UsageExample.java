package com.ww.app.redis.component.rank;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 排行榜组件使用示例
 * 
 * @author ww
 * @create 2025-11-25
 * @description: 展示如何使用排行榜组件的各种功能
 */
@Slf4j
@Service
public class UsageExample {

    @Resource
    private RankingRedisComponent rankingRedisComponent;

    /**
     * 示例1：游戏排行榜
     */
    public void gameRankingExample() {
        String bizType = "game";
        String bizId = "level1";
        
        // 添加玩家分数
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, "player1", 1000.0);
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, "player2", 2000.0);
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, "player3", 1500.0);
        
        // 获取TOP 50排行榜
        List<RankingItem> top50 = rankingRedisComponent.getRanking(bizType, bizId);
        log.info("游戏排行榜TOP 50: {}", top50);
        
        // 获取玩家排名
        long rank = rankingRedisComponent.getMemberRank(bizType, bizId, "player1");
        log.info("玩家1的排名: {}", rank);
        
        // 玩家得分增加
        rankingRedisComponent.incrementScore(bizType, bizId, "player1", 500.0);
        
        // 再次查询排名
        rank = rankingRedisComponent.getMemberRank(bizType, bizId, "player1");
        log.info("玩家1的新排名: {}", rank);
    }

    /**
     * 示例2：活动排行榜（带过期时间）
     */
    public void activityRankingExample() {
        String bizType = "activity";
        String bizId = "spring2024";
        
        // 批量添加用户积分
        Map<String, Double> userPoints = new HashMap<>();
        userPoints.put("user1", 100.0);
        userPoints.put("user2", 200.0);
        userPoints.put("user3", 150.0);
        userPoints.put("user4", 300.0);
        userPoints.put("user5", 250.0);
        
        rankingRedisComponent.batchAddOrUpdateScore(bizType, bizId, userPoints);
        
        // 设置7天后过期
        rankingRedisComponent.expireRanking(bizType, bizId, 7 * 24 * 3600);
        
        // 分页获取排行榜
        List<RankingItem> firstPage = rankingRedisComponent.getRanking(bizType, bizId, 1, 10, true);
        log.info("活动排行榜第一页: {}", firstPage);
        
        // 获取排行榜总数
        long total = rankingRedisComponent.getRankingCount(bizType, bizId);
        log.info("活动排行榜总数: {}", total);
    }

    /**
     * 示例3：商品销量排行榜
     */
    public void productSalesRankingExample() {
        String bizType = "product";
        String bizId = "sales";
        
        // 商品销量增加
        rankingRedisComponent.incrementScore(bizType, bizId, "product1", 1.0);
        rankingRedisComponent.incrementScore(bizType, bizId, "product2", 1.0);
        rankingRedisComponent.incrementScore(bizType, bizId, "product1", 1.0);
        
        // 获取销量TOP 10
        List<RankingItem> top10 = rankingRedisComponent.getRankingByRankRange(bizType, bizId, 1, 10, true);
        log.info("商品销量TOP 10: {}", top10);
        
        // 查询商品1的销量和排名
        RankingItem product1Info = rankingRedisComponent.getMemberRankingInfo(bizType, bizId, "product1", true);
        if (product1Info != null) {
            log.info("商品1 - 排名: {}, 销量: {}", product1Info.getRank(), product1Info.getScore());
        }
    }

    /**
     * 示例4：按分数区间查询
     */
    public void scoreRangeExample() {
        String bizType = "game";
        String bizId = "level1";
        
        // 查询分数在1000-2000之间的玩家数量
        long count = rankingRedisComponent.countByScoreRange(bizType, bizId, 1000.0, 2000.0);
        log.info("分数在1000-2000之间的玩家数量: {}", count);
    }

    /**
     * 示例5：批量删除
     */
    public void batchDeleteExample() {
        String bizType = "activity";
        String bizId = "spring2024";
        
        // 批量删除成员
        List<String> memberIds = Arrays.asList("user1", "user2", "user3");
        long removed = rankingRedisComponent.batchRemoveMembers(bizType, bizId, memberIds);
        log.info("批量删除成员数量: {}", removed);
        
        // 删除整个排行榜
        boolean deleted = rankingRedisComponent.deleteRanking(bizType, bizId);
        log.info("删除排行榜结果: {}", deleted);
    }

    /**
     * 示例6：获取成员详细信息
     */
    public void memberInfoExample() {
        String bizType = "game";
        String bizId = "level1";
        String memberId = "player1";
        
        // 获取成员完整信息（排名+分数）
        RankingItem info = rankingRedisComponent.getMemberRankingInfo(bizType, bizId, memberId, true);
        if (info != null) {
            log.info("成员信息 - ID: {}, 排名: {}, 分数: {}", 
                    info.getMemberId(), info.getRank(), info.getScore());
        } else {
            log.info("成员不存在或未上榜");
        }
        
        // 单独获取排名
        long rank = rankingRedisComponent.getMemberRank(bizType, bizId, memberId);
        log.info("成员排名: {}", rank);
        
        // 单独获取分数
        Double score = rankingRedisComponent.getMemberScore(bizType, bizId, memberId);
        log.info("成员分数: {}", score);
    }
}

