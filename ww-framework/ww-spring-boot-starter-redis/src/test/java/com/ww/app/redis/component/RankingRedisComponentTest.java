package com.ww.app.redis.component;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.ww.app.redis.RedisTestApplication;
import com.ww.app.redis.component.key.RankingRedisKeyBuilder;
import com.ww.app.redis.component.rank.RankingItem;
import com.ww.app.redis.component.rank.RankingRedisComponent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.*;

/**
 * 排行榜组件集成测试
 * 覆盖新增、增量、批量、查询、分片等核心场景
 *
 * @author ww
 */
@Slf4j
@SpringBootTest(classes = RedisTestApplication.class)
public class RankingRedisComponentTest {

    @Resource
    private RankingRedisComponent rankingRedisComponent;

    @Resource
    private RankingRedisKeyBuilder rankingRedisKeyBuilder;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private String bizType;
    private String bizId;

    @BeforeEach
    public void setUp() {
        bizType = "rank_test";
        bizId = "scene_" + RandomUtil.randomNumbers(6);
        cleanup();
    }

    @AfterEach
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        String keyPrefix = rankingRedisKeyBuilder.buildRankingKey(bizType, bizId);
        Set<String> keys = stringRedisTemplate.keys(keyPrefix + "*");
        if (CollUtil.isNotEmpty(keys)) {
            stringRedisTemplate.delete(keys);
        }
    }

    @Test
    public void testAddAndGetRanking() {
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, "userA", 100);
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, "userB", 200);
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, "userC", 150);

        List<RankingItem> top = rankingRedisComponent.getRanking(bizType, bizId);
        Assertions.assertEquals(3, top.size(), "排行榜应包含3个成员");
        Assertions.assertEquals("userB", top.get(0).getMemberId(), "最高分应为userB");

        long rank = rankingRedisComponent.getMemberRank(bizType, bizId, "userC");
        Assertions.assertEquals(2, rank, "userC排名应为2");

        Double score = rankingRedisComponent.getMemberScore(bizType, bizId, "userA");
        Assertions.assertEquals(100d, score, 0.01, "userA分数应为100");
    }

    @Test
    public void testIncrementScoreAndMemberInfo() {
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, "userA", 50);
        rankingRedisComponent.incrementScore(bizType, bizId, "userA", 25);

        RankingItem info = rankingRedisComponent.getMemberRankingInfo(bizType, bizId, "userA", true);
        Assertions.assertNotNull(info, "成员信息不应为空");
        Assertions.assertEquals(75d, info.getScore(), 0.01, "增量计算错误");
        Assertions.assertEquals(1L, info.getRank(), "排名应为1");
    }

    @Test
    public void testBatchAddAndCount() {
        Map<String, Double> scores = new HashMap<>();
        scores.put("user1", 10d);
        scores.put("user2", 20d);
        scores.put("user3", 30d);
        scores.put("user4", 40d);

        long added = rankingRedisComponent.batchAddOrUpdateScore(bizType, bizId, scores);
        Assertions.assertEquals(scores.size(), added, "批量添加数量不一致");

        long count = rankingRedisComponent.getRankingCount(bizType, bizId);
        Assertions.assertEquals(scores.size(), count, "排行榜总数不匹配");

        List<RankingItem> page = rankingRedisComponent.getRanking(bizType, bizId, 1, 2, true);
        Assertions.assertEquals(2, page.size(), "分页结果数量错误");
        Assertions.assertEquals("user4", page.get(0).getMemberId(), "分页排序错误");
    }

    @Test
    public void testRemoveAndDeleteRanking() {
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, "userA", 100);
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, "userB", 120);

        boolean removed = rankingRedisComponent.removeMember(bizType, bizId, "userA");
        Assertions.assertTrue(removed, "删除成员失败");

        long count = rankingRedisComponent.getRankingCount(bizType, bizId);
        Assertions.assertEquals(1, count, "删除成员后数量不正确");

        boolean deleted = rankingRedisComponent.deleteRanking(bizType, bizId);
        Assertions.assertTrue(deleted, "删除排行榜失败");
    }

    @Test
    public void testCountByScoreRangeAndRankRange() {
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, "user1", 50);
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, "user2", 150);
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, "user3", 250);

        long count = rankingRedisComponent.countByScoreRange(bizType, bizId, 100, 300);
        Assertions.assertEquals(2, count, "分数区间统计结果错误");

        List<RankingItem> items = rankingRedisComponent.getRankingByRankRange(bizType, bizId, 1, 2, true);
        Assertions.assertEquals(2, items.size(), "排名区间结果数量错误");
        Assertions.assertEquals("user3", items.get(0).getMemberId(), "排名区间排序错误");
    }

    @Test
    public void testAutoShardingForLargeDataset() {
        Map<String, Double> scores = new HashMap<>();
        int members = 10500;
        for (int i = 0; i < members; i++) {
            scores.put("user_" + i, (double) i);
        }

        long added = rankingRedisComponent.batchAddOrUpdateScore(bizType, bizId, scores);
        Assertions.assertEquals(members, added, "批量添加失败");

        long count = rankingRedisComponent.getRankingCount(bizType, bizId);
        Assertions.assertEquals(members, count, "分片后统计失败");

        String baseKey = rankingRedisKeyBuilder.buildRankingKey(bizType, bizId);
        Boolean shardingFlag = stringRedisTemplate.hasKey(baseKey + ":sharding:flag");
        Assertions.assertTrue(shardingFlag, "应开启分片");

        RankingItem top = rankingRedisComponent.getRanking(bizType, bizId, 1, 1, true).get(0);
        Assertions.assertEquals("user_" + (members - 1), top.getMemberId(), "分片排行榜排序错误");
    }
}

