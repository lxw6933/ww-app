package com.ww.app.redis.component.rank;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import com.ww.app.redis.RedisTestApplication;
import com.ww.app.redis.component.key.RankingRedisKeyBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * RankingRedisComponent 测试类
 * 覆盖常见业务场景与边界场景
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
    private String baseKey;

    private String memberId1;
    private String memberId2;
    private String memberId3;

    @BeforeEach
    public void setUp() {
        bizType = "rank_test_" + RandomUtil.randomNumbers(4);
        bizId = "biz_" + RandomUtil.randomNumbers(6);
        baseKey = rankingRedisKeyBuilder.buildRankingKey(bizType, bizId);
        memberId1 = "user_" + RandomUtil.randomNumbers(6);
        memberId2 = "user_" + RandomUtil.randomNumbers(6);
        memberId3 = "user_" + RandomUtil.randomNumbers(6);
        cleanup();
    }

    @AfterEach
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        // 清理主key、分片key与分片标记
        Set<String> keys = new HashSet<>();
        keys.add(baseKey);
        keys.add(getShardingFlagKey(baseKey));
        for (int i = 0; i < 64; i++) {
            keys.add(buildShardKey(baseKey, i));
        }
        stringRedisTemplate.delete(keys);
    }

    private String getShardingFlagKey(String key) {
        return key + ":sharding:flag";
    }

    private String buildShardKey(String key, int shardIndex) {
        return key + ":shard:" + shardIndex;
    }

    private int getShardIndex(String memberId) {
        return Math.floorMod(memberId.hashCode(), 64);
    }

    /**
     * 测试写入、查询与排序
     */
    @Test
    public void testAddOrUpdateAndRanking() {
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId1, 100);
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId2, 300);
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId3, 200);

        List<RankingItem> ranking = rankingRedisComponent.getRanking(bizType, bizId, 1, 10, true);
        Assert.isTrue(ranking.size() == 3, "排行榜数量应为3");
        Assert.isTrue(memberId2.equals(ranking.get(0).getMemberId()), "第一名应为memberId2");
        Assert.isTrue(memberId3.equals(ranking.get(1).getMemberId()), "第二名应为memberId3");
        Assert.isTrue(memberId1.equals(ranking.get(2).getMemberId()), "第三名应为memberId1");

        long rank = rankingRedisComponent.getMemberRank(bizType, bizId, memberId2);
        Assert.isTrue(rank == 1, "memberId2排名应为1");

        Double score = rankingRedisComponent.getMemberScore(bizType, bizId, memberId1);
        Assert.isTrue(score != null && score == 100, "memberId1分数应为100");
    }

    /**
     * 测试相同分数按时间先后排序
     */
    @Test
    public void testTieScoreOrderByTime() throws Exception {
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId1, 100);
        Thread.sleep(2);
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId2, 100);

        List<RankingItem> ranking = rankingRedisComponent.getRanking(bizType, bizId, 1, 10, true);
        Assert.isTrue(ranking.size() == 2, "排行榜数量应为2");
        Assert.isTrue(memberId1.equals(ranking.get(0).getMemberId()), "相同分数时先写入应排前");
    }

    /**
     * 测试增量更新分数
     */
    @Test
    public void testIncrementScore() {
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId1, 100);
        Double finalScore = rankingRedisComponent.incrementScore(bizType, bizId, memberId1, 20);
        Assert.isTrue(finalScore != null, "增量更新应返回最终分数");

        Double score = rankingRedisComponent.getMemberScore(bizType, bizId, memberId1);
        Assert.isTrue(score != null && score == 120, "增量更新后分数应为120");
    }

    /**
     * 测试增量更新在成员不存在时自动创建
     */
    @Test
    public void testIncrementScoreCreateMember() {
        Double finalScore = rankingRedisComponent.incrementScore(bizType, bizId, memberId1, 15);
        Assert.isTrue(finalScore != null, "成员不存在时增量更新应创建成员");

        Double score = rankingRedisComponent.getMemberScore(bizType, bizId, memberId1);
        Assert.isTrue(score != null && score == 15, "增量更新后分数应为15");
    }

    /**
     * 测试批量写入与统计
     */
    @Test
    public void testBatchAddAndCount() {
        Map<String, Double> scores = new HashMap<>();
        scores.put(memberId1, 100.0);
        scores.put(memberId2, 150.0);
        scores.put(memberId3, 200.0);
        long added = rankingRedisComponent.batchAddOrUpdateScore(bizType, bizId, scores);
        Assert.isTrue(added == 3, "批量写入数量应为3");

        long total = rankingRedisComponent.getRankingCount(bizType, bizId);
        Assert.isTrue(total == 3, "排行榜总数应为3");

        long count = rankingRedisComponent.countByScoreRange(bizType, bizId, 100, 150);
        Assert.isTrue(count == 2, "分数区间统计应为2");
    }

    /**
     * 测试按区间获取排行榜
     */
    @Test
    public void testGetRankingByRange() {
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId1, 10);
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId2, 20);
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId3, 30);

        List<RankingItem> ranking = rankingRedisComponent.getRankingByRankRange(bizType, bizId, 1, 2, true);
        Assert.isTrue(ranking.size() == 2, "区间排行数量应为2");
        Assert.isTrue(memberId3.equals(ranking.get(0).getMemberId()), "区间排行第一名应为memberId3");
    }

    /**
     * 测试升序排名
     */
    @Test
    public void testAscendingRanking() {
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId1, 10);
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId2, 20);
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId3, 30);

        List<RankingItem> ranking = rankingRedisComponent.getRanking(bizType, bizId, 1, 10, false);
        Assert.isTrue(ranking.size() == 3, "升序排行榜数量应为3");
        Assert.isTrue(memberId1.equals(ranking.get(0).getMemberId()), "升序第一名应为memberId1");
    }

    /**
     * 测试成员排行信息
     */
    @Test
    public void testGetMemberRankingInfo() {
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId1, 100);
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId2, 200);

        RankingItem item = rankingRedisComponent.getMemberRankingInfo(bizType, bizId, memberId1, true);
        Assert.isTrue(item != null, "成员排行信息不应为空");
        Assert.isTrue(item.getRank() == 2, "memberId1排名应为2");
        Assert.isTrue(item.getScore() == 100, "memberId1分数应为100");
    }

    /**
     * 测试删除与批量删除
     */
    @Test
    public void testRemoveOperations() {
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId1, 100);
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId2, 200);
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId3, 300);

        boolean removed = rankingRedisComponent.removeMember(bizType, bizId, memberId1);
        Assert.isTrue(removed, "删除单个成员应成功");

        long removedCount = rankingRedisComponent.batchRemoveMembers(bizType, bizId, Arrays.asList(memberId2, memberId3));
        Assert.isTrue(removedCount == 2, "批量删除应删除2个成员");

        long total = rankingRedisComponent.getRankingCount(bizType, bizId);
        Assert.isTrue(total == 0, "删除后排行榜总数应为0");
    }

    /**
     * 测试过期与删除榜单
     */
    @Test
    public void testExpireAndDeleteRanking() {
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId1, 100);
        boolean expired = rankingRedisComponent.expireRanking(bizType, bizId, 5);
        Assert.isTrue(expired, "设置过期时间应成功");

        Long ttl = stringRedisTemplate.getExpire(baseKey, TimeUnit.SECONDS);
        Assert.isTrue(ttl > 0, "过期时间应大于0");

        boolean deleted = rankingRedisComponent.deleteRanking(bizType, bizId);
        Assert.isTrue(deleted, "删除排行榜应成功");
    }

    /**
     * 测试删除榜单时清理分片key与标记
     */
    @Test
    public void testDeleteRankingWithShardKeys() {
        stringRedisTemplate.opsForValue().set(getShardingFlagKey(baseKey), "1");
        stringRedisTemplate.opsForZSet().add(buildShardKey(baseKey, 1), memberId1, 100 * 1e8);
        stringRedisTemplate.opsForZSet().add(buildShardKey(baseKey, 2), memberId2, 200 * 1e8);

        boolean deleted = rankingRedisComponent.deleteRanking(bizType, bizId);
        Assert.isTrue(deleted, "删除排行榜应成功");
        stringRedisTemplate.hasKey(getShardingFlagKey(baseKey));
        Assert.isTrue(!stringRedisTemplate.hasKey(getShardingFlagKey(baseKey)), "分片标记应被删除");
        Assert.isTrue(stringRedisTemplate.opsForZSet().zCard(buildShardKey(baseKey, 1)) == 0, "分片key应被清理");
    }

    /**
     * 测试分片启用场景
     */
    @Test
    public void testShardingEnabledRead() {
        // 按真实逻辑触发分片：写入超过阈值的数据
        int totalMembers = 10005;
        for (int i = 0; i < totalMembers; i++) {
            String memberId = "shard_user_" + i;
            rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId, i);
        }
        String shardFlag = stringRedisTemplate.opsForValue().get(getShardingFlagKey(baseKey));
        Assert.isTrue("1".equals(shardFlag), "达到阈值后应启用分片");

        List<RankingItem> ranking = rankingRedisComponent.getRanking(bizType, bizId);
        Assert.isTrue(!ranking.isEmpty(), "分片启用后排行榜结果不应为空");
    }

    /**
     * 测试分片写入与读取成员信息
     */
    @Test
    public void testShardingWriteAndMemberInfo() {
        stringRedisTemplate.opsForValue().set(getShardingFlagKey(baseKey), "1");
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId1, 100);
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId2, 200);

        RankingItem item = rankingRedisComponent.getMemberRankingInfo(bizType, bizId, memberId1, true);
        Assert.isTrue(item != null, "分片模式下应获取成员排行信息");
        Assert.isTrue(item.getScore() == 100, "分片模式下成员分数应为100");
    }

    /**
     * 测试迁移中读取仍走主key
     */
    @Test
    public void testShardingMigratingReadMain() {
        stringRedisTemplate.opsForValue().set(getShardingFlagKey(baseKey), "2");

        // 仅向分片写入，模拟迁移未完成
        int shardIndex = getShardIndex(memberId1);
        stringRedisTemplate.opsForZSet().add(buildShardKey(baseKey, shardIndex), memberId1, 100 * 1e8);

        List<RankingItem> emptyRanking = rankingRedisComponent.getRanking(bizType, bizId);
        Assert.isTrue(emptyRanking.isEmpty(), "迁移中读取主key，应忽略未迁移分片数据");

        // 正常写入（双写），主key可读取到
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId1, 100);
        List<RankingItem> ranking = rankingRedisComponent.getRanking(bizType, bizId);
        Assert.isTrue(ranking.size() == 1, "迁移中双写后主key应可读取");
    }

    /**
     * 测试迁移中双写主key与分片
     */
    @Test
    public void testShardingMigratingDualWrite() {
        stringRedisTemplate.opsForValue().set(getShardingFlagKey(baseKey), "2");
        rankingRedisComponent.addOrUpdateScore(bizType, bizId, memberId1, 100);

        Double mainScore = stringRedisTemplate.opsForZSet().score(baseKey, memberId1);
        Double shardScore = stringRedisTemplate.opsForZSet()
                .score(buildShardKey(baseKey, getShardIndex(memberId1)), memberId1);
        Assert.isTrue(mainScore != null, "迁移中主key应写入");
        Assert.isTrue(shardScore != null, "迁移中分片应写入");
    }

    /**
     * 测试分片场景下的统计与分页边界
     */
    @Test
    public void testShardCountAndPageLimit() {
        int totalMembers = 1100;
        for (int i = 0; i < totalMembers; i++) {
            rankingRedisComponent.addOrUpdateScore(bizType, bizId, "page_user_" + i, i);
        }
        stringRedisTemplate.opsForValue().set(getShardingFlagKey(baseKey), "1");

        long total = rankingRedisComponent.getRankingCount(bizType, bizId);
        Assert.isTrue(total > 0, "分片场景下总数应大于0");

        List<RankingItem> ranking = rankingRedisComponent.getRanking(bizType, bizId, 1, 2000, true);
        Assert.isTrue(ranking.size() <= 1000, "分页大小应被限制在1000以内");
    }

    /**
     * 测试参数校验与边界场景
     */
    @Test
    public void testParameterValidation() {
        Assert.isFalse(rankingRedisComponent.addOrUpdateScore("", bizId, memberId1, 100), "空bizType应返回false");
        Assert.isTrue(rankingRedisComponent.incrementScore("", bizId, memberId1, 10) == null, "空bizType应返回null");
        Assert.isTrue(rankingRedisComponent.getRanking("", bizId).isEmpty(), "空bizType应返回空列表");
        Assert.isTrue(rankingRedisComponent.getMemberRank("", bizId, memberId1) == 0, "空bizType应返回0");
        Assert.isTrue(rankingRedisComponent.getMemberScore("", bizId, memberId1) == null, "空bizType应返回null");
        Assert.isFalse(rankingRedisComponent.expireRanking("", bizId, 10), "空bizType应返回false");
        Assert.isTrue(rankingRedisComponent.countByScoreRange("", bizId, 1, 2) == 0, "空bizType应返回0");
        Assert.isFalse(rankingRedisComponent.removeMember("", bizId, memberId1), "空bizType应返回false");
        Assert.isTrue(rankingRedisComponent.batchAddOrUpdateScore("", bizId, Collections.emptyMap()) == 0, "空参数应返回0");
        Assert.isTrue(rankingRedisComponent.batchRemoveMembers("", bizId, Collections.singletonList(memberId1)) == 0, "空bizType应返回0");
        Assert.isTrue(rankingRedisComponent.getRankingByRankRange("", bizId, 1, 2, true).isEmpty(), "空bizType应返回空列表");
        Assert.isTrue(rankingRedisComponent.getRankingByRankRange(bizType, bizId, 0, 2, true).isEmpty(), "非法分页应返回空列表");
    }
}
