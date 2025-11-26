package com.ww.app.redis.component.rank;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ww.app.redis.component.key.RankingRedisKeyBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 通用排行榜组件
 * <p>
 * 基于Redis Sorted Set实现，支持高性能、高并发的排行榜功能
 * <p>
 * 特性：
 * - 默认最多展示50条
 * - 支持分页查询
 * - 支持批量操作
 * - 支持过期时间设置
 * - 支持升序/降序排序
 * - 支持获取用户排名和分数
 * - 完善的异常处理和日志记录
 * 
 * @author ww
 * @create 2025-11-25 9:10
 * @description: 通用排行榜Redis组件，提供生产级别的高性能排行榜功能
 */
@Slf4j
@Component
public class RankingRedisComponent {

    /**
     * 默认排行榜展示数量
     */
    private static final int DEFAULT_TOP_SIZE = 50;

    /**
     * 默认分页大小
     */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 最大分页大小
     */
    private static final int MAX_PAGE_SIZE = 1000;

    /**
     * 批量操作大小
     */
    private static final int BATCH_SIZE = 1000;

    /**
     * 分数倍数，用于将原始分数和时间戳组合
     * 使用1e8可以支持最大99,999,999的分数范围（约1亿）
     * 满足充值金额等业务场景（100万 * 100倍 = 1亿）
     */
    private static final double SCORE_MULTIPLIER = 1e8;

    /**
     * 时间戳后缀模数（毫秒时间戳的后8位，约支持2.7小时的时间窗口）
     * 使用后8位可以支持足够的时间范围，同时保证精度
     */
    private static final long TIME_SUFFIX_MOD = 100000000L; // 10^8

    /**
     * 分片数量，用于避免bigkey问题
     * 当用户量超过阈值时，将数据分散到多个zset中
     */
    private static final int SHARD_COUNT = 64;

    /**
     * 启用分片的阈值（用户数量）
     * 当排行榜成员数超过此阈值时，自动启用分片
     */
    private static final long SHARD_THRESHOLD = 10000;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RankingRedisKeyBuilder rankingRedisKeyBuilder;

    /**
     * 计算最终分数
     * 将原始分数和时间戳组合，确保相同分数时按时间排序（先达到的排在前面）
     * <p>
     * 算法说明：
     * 1. 使用1e8作为倍数，支持最大99,999,999的分数范围（约1亿）
     * 2. 时间戳后缀使用毫秒时间戳的后8位 + 纳秒时间戳的后3位
     *    这样可以处理同一毫秒内的并发情况，同时支持约2.7小时的时间窗口
     * 3. 使用反转的方式，让时间早的排在前面
     * 
     * @param score 原始分数
     * @return 最终分数
     */
    private double calculateFinalScore(double score) {
        // 获取当前时间戳（毫秒）
        long currentTime = System.currentTimeMillis();
        // 获取纳秒时间戳的后3位，用于处理同一毫秒内的并发情况
        long nanoSuffix = System.nanoTime() % 1000;
        // 时间戳后缀 = 毫秒时间戳后8位 + 纳秒后3位（组合成11位，确保唯一性）
        // 使用反转的方式，让时间早的排在前面
        long timeSuffix = (currentTime % TIME_SUFFIX_MOD) * 1000 + nanoSuffix;
        // 最终分数 = 原始分数 * 1e8 + (1e11 - 时间戳后缀) / 1e8
        // 这样既保留了原始分数的大小关系，又能按时间排序
        return score * SCORE_MULTIPLIER + (1e11 - timeSuffix) / SCORE_MULTIPLIER;
    }

    /**
     * 获取分片索引
     * 根据memberId的hash值计算分片索引
     * 
     * @param memberId 成员ID
     * @return 分片索引（0到SHARD_COUNT-1）
     */
    private int getShardIndex(String memberId) {
        return Math.abs(memberId.hashCode()) % SHARD_COUNT;
    }

    /**
     * 构建分片key
     * 
     * @param baseKey 基础key
     * @param shardIndex 分片索引
     * @return 分片key
     */
    private String buildShardKey(String baseKey, int shardIndex) {
        return baseKey + ":shard:" + shardIndex;
    }

    /**
     * 检查是否需要启用分片
     * 
     * @param key 基础key
     * @return 是否需要分片
     */
    private boolean needSharding(String key) {
        try {
            Long count = stringRedisTemplate.opsForZSet().zCard(key);
            return count != null && count >= SHARD_THRESHOLD;
        } catch (Exception e) {
            log.error("检查分片状态异常: key={}", key, e);
            return false;
        }
    }

    /**
     * 获取分片标记key
     * 
     * @param baseKey 基础key
     * @return 分片标记key
     */
    private String getShardingFlagKey(String baseKey) {
        return baseKey + ":sharding:flag";
    }

    /**
     * 还原原始分数
     * 
     * @param finalScore 最终分数
     * @return 原始分数
     */
    private double restoreOriginalScore(double finalScore) {
        // 直接取整数部分作为原始分数
        return Math.floor(finalScore / SCORE_MULTIPLIER);
    }

    /**
     * 检查是否已启用分片
     * 
     * @param baseKey 基础key
     * @return 是否已启用分片
     */
    private boolean isShardingEnabled(String baseKey) {
        try {
            String flagKey = getShardingFlagKey(baseKey);
            String flag = stringRedisTemplate.opsForValue().get(flagKey);
            return "1".equals(flag);
        } catch (Exception e) {
            log.error("检查分片标记异常: baseKey={}", baseKey, e);
            return false;
        }
    }

    /**
     * 启用分片并迁移数据
     * 
     * @param baseKey 基础key
     */
    private void enableSharding(String baseKey) {
        try {
            // 检查是否已经启用分片
            if (isShardingEnabled(baseKey)) {
                return;
            }

            // 获取所有成员
            Set<ZSetOperations.TypedTuple<String>> allMembers = 
                    stringRedisTemplate.opsForZSet().rangeWithScores(baseKey, 0, -1);
            
            if (CollUtil.isEmpty(allMembers)) {
                return;
            }

            // 将数据迁移到分片
            Map<Integer, Set<ZSetOperations.TypedTuple<String>>> shardMap = new HashMap<>();
            for (ZSetOperations.TypedTuple<String> tuple : allMembers) {
                if (tuple != null && tuple.getValue() != null) {
                    int shardIndex = getShardIndex(tuple.getValue());
                    shardMap.computeIfAbsent(shardIndex, k -> new HashSet<>()).add(tuple);
                }
            }

            // 批量写入分片
            for (Map.Entry<Integer, Set<ZSetOperations.TypedTuple<String>>> entry : shardMap.entrySet()) {
                String shardKey = buildShardKey(baseKey, entry.getKey());
                stringRedisTemplate.opsForZSet().add(shardKey, entry.getValue());
            }

            // 设置分片标记
            String flagKey = getShardingFlagKey(baseKey);
            stringRedisTemplate.opsForValue().set(flagKey, "1");

            // 删除原始key（可选，根据业务需求决定）
            // stringRedisTemplate.delete(baseKey);

            log.info("启用分片成功: baseKey={}, shardCount={}, totalMembers={}", 
                    baseKey, shardMap.size(), allMembers.size());
        } catch (Exception e) {
            log.error("启用分片异常: baseKey={}", baseKey, e);
        }
    }

    /**
     * 添加或更新成员分数
     * 
     * @param bizType 业务类型
     * @param bizId 业务ID（可选）
     * @param memberId 成员ID
     * @param score 分数
     * @return 是否成功
     */
    public boolean addOrUpdateScore(String bizType, String bizId, String memberId, double score) {
        if (StrUtil.isBlank(bizType) || StrUtil.isBlank(memberId)) {
            log.warn("添加排行榜分数参数无效: bizType={}, memberId={}", bizType, memberId);
            return false;
        }

        try {
            String baseKey = rankingRedisKeyBuilder.buildRankingKey(bizType, bizId);
            double finalScore = calculateFinalScore(score);
            
            // 检查是否已启用分片
            if (isShardingEnabled(baseKey)) {
                // 已启用分片，直接写入分片
                int shardIndex = getShardIndex(memberId);
                String shardKey = buildShardKey(baseKey, shardIndex);
                Boolean result = stringRedisTemplate.opsForZSet().add(shardKey, memberId, finalScore);
                log.debug("添加排行榜分数(分片): bizType={}, bizId={}, memberId={}, score={}, shardIndex={}, result={}", 
                        bizType, bizId, memberId, score, shardIndex, result);
                return Boolean.TRUE.equals(result);
            } else {
                // 未启用分片，写入主key
                Boolean result = stringRedisTemplate.opsForZSet().add(baseKey, memberId, finalScore);
                
                // 检查是否需要启用分片
                if (needSharding(baseKey)) {
                    // 异步启用分片（避免阻塞）
                    enableSharding(baseKey);
                }
                
                log.debug("添加排行榜分数: bizType={}, bizId={}, memberId={}, score={}, result={}", 
                        bizType, bizId, memberId, score, result);
                return Boolean.TRUE.equals(result);
            }
        } catch (Exception e) {
            log.error("添加排行榜分数异常: bizType={}, bizId={}, memberId={}, score={}", 
                    bizType, bizId, memberId, score, e);
            return false;
        }
    }

    /**
     * 增加成员分数（增量更新）
     * 
     * @param bizType 业务类型
     * @param bizId 业务ID（可选）
     * @param memberId 成员ID
     * @param increment 增量分数
     * @return 更新后的分数
     */
    public Double incrementScore(String bizType, String bizId, String memberId, double increment) {
        if (StrUtil.isBlank(bizType) || StrUtil.isBlank(memberId)) {
            log.warn("增加排行榜分数参数无效: bizType={}, memberId={}", bizType, memberId);
            return null;
        }

        try {
            String baseKey = rankingRedisKeyBuilder.buildRankingKey(bizType, bizId);
            
            // 检查是否已启用分片
            if (isShardingEnabled(baseKey)) {
                // 已启用分片，操作分片
                int shardIndex = getShardIndex(memberId);
                String shardKey = buildShardKey(baseKey, shardIndex);
                // 注意：incrementScore会直接增加分数，但我们需要保持时间戳后缀
                // 所以先获取当前分数，然后重新计算
                Double currentFinalScore = stringRedisTemplate.opsForZSet().score(shardKey, memberId);
                if (currentFinalScore != null) {
                    double currentOriginalScore = restoreOriginalScore(currentFinalScore);
                    double newOriginalScore = currentOriginalScore + increment;
                    double newFinalScore = calculateFinalScore(newOriginalScore);
                    stringRedisTemplate.opsForZSet().add(shardKey, memberId, newFinalScore);
                    return newFinalScore;
                } else {
                    // 成员不存在，直接添加
                    double newFinalScore = calculateFinalScore(increment);
                    stringRedisTemplate.opsForZSet().add(shardKey, memberId, newFinalScore);
                    return newFinalScore;
                }
            } else {
                // 未启用分片，操作主key
                Double newScore = stringRedisTemplate.opsForZSet().incrementScore(baseKey, memberId, increment);
                log.debug("增加排行榜分数: bizType={}, bizId={}, memberId={}, increment={}, newScore={}", 
                        bizType, bizId, memberId, increment, newScore);
                return newScore;
            }
        } catch (Exception e) {
            log.error("增加排行榜分数异常: bizType={}, bizId={}, memberId={}, increment={}", 
                    bizType, bizId, memberId, increment, e);
            return null;
        }
    }

    /**
     * 批量添加或更新成员分数
     * 
     * @param bizType 业务类型
     * @param bizId 业务ID（可选）
     * @param memberScores 成员分数Map，key为memberId，value为score
     * @return 成功添加的数量
     */
    public long batchAddOrUpdateScore(String bizType, String bizId, Map<String, Double> memberScores) {
        if (StrUtil.isBlank(bizType) || CollUtil.isEmpty(memberScores)) {
            log.warn("批量添加排行榜分数参数无效: bizType={}, size={}", bizType, 
                    memberScores != null ? memberScores.size() : 0);
            return 0;
        }

        try {
            String baseKey = rankingRedisKeyBuilder.buildRankingKey(bizType, bizId);
            boolean shardingEnabled = isShardingEnabled(baseKey);
            
            // 按分片分组
            Map<Integer, Set<ZSetOperations.TypedTuple<String>>> shardMap = new HashMap<>();
            Set<ZSetOperations.TypedTuple<String>> mainTuples = new HashSet<>();
            
            for (Map.Entry<String, Double> entry : memberScores.entrySet()) {
                if (StrUtil.isNotBlank(entry.getKey()) && entry.getValue() != null) {
                    double score = entry.getValue();
                    double finalScore = calculateFinalScore(score);
                    DefaultTypedTuple<String> tuple = new DefaultTypedTuple<>(entry.getKey(), finalScore);
                    
                    if (shardingEnabled) {
                        int shardIndex = getShardIndex(entry.getKey());
                        shardMap.computeIfAbsent(shardIndex, k -> new HashSet<>()).add(tuple);
                    } else {
                        mainTuples.add(tuple);
                    }
                }
            }

            long totalAdded = 0;
            
            if (shardingEnabled) {
                // 已启用分片，按分片批量添加
                for (Map.Entry<Integer, Set<ZSetOperations.TypedTuple<String>>> entry : shardMap.entrySet()) {
                    String shardKey = buildShardKey(baseKey, entry.getKey());
                    Set<ZSetOperations.TypedTuple<String>> tuples = entry.getValue();
                    
                    // 分批处理
                    List<ZSetOperations.TypedTuple<String>> tupleList = new ArrayList<>(tuples);
                    for (int i = 0; i < tupleList.size(); i += BATCH_SIZE) {
                        int end = Math.min(i + BATCH_SIZE, tupleList.size());
                        Set<ZSetOperations.TypedTuple<String>> batch = new HashSet<>(tupleList.subList(i, end));
                        Long added = stringRedisTemplate.opsForZSet().add(shardKey, batch);
                        if (added != null) {
                            totalAdded += added;
                        }
                    }
                }
            } else {
                // 未启用分片，添加到主key
                if (!mainTuples.isEmpty()) {
                    List<ZSetOperations.TypedTuple<String>> tupleList = new ArrayList<>(mainTuples);
                    for (int i = 0; i < tupleList.size(); i += BATCH_SIZE) {
                        int end = Math.min(i + BATCH_SIZE, tupleList.size());
                        Set<ZSetOperations.TypedTuple<String>> batch = new HashSet<>(tupleList.subList(i, end));
                        Long added = stringRedisTemplate.opsForZSet().add(baseKey, batch);
                        if (added != null) {
                            totalAdded += added;
                        }
                    }
                    
                    // 检查是否需要启用分片
                    if (needSharding(baseKey)) {
                        enableSharding(baseKey);
                    }
                }
            }

            log.debug("批量添加排行榜分数: bizType={}, bizId={}, size={}, added={}, sharding={}", 
                    bizType, bizId, memberScores.size(), totalAdded, shardingEnabled);
            return totalAdded;
        } catch (Exception e) {
            log.error("批量添加排行榜分数异常: bizType={}, bizId={}, size={}", 
                    bizType, bizId, memberScores.size(), e);
            return 0;
        }
    }

    /**
     * 从分片中获取排行榜并合并
     * 
     * @param baseKey 基础key
     * @param start 起始位置
     * @param end 结束位置
     * @param desc 是否降序
     * @return 排行榜列表
     */
    private List<RankingItem> getRankingFromShards(String baseKey, long start, long end, boolean desc) {
        // 从所有分片中获取数据
        List<ZSetOperations.TypedTuple<String>> allTuples = new ArrayList<>();
        
        for (int i = 0; i < SHARD_COUNT; i++) {
            String shardKey = buildShardKey(baseKey, i);
            Set<ZSetOperations.TypedTuple<String>> tuples;
            if (desc) {
                tuples = stringRedisTemplate.opsForZSet().reverseRangeWithScores(shardKey, 0, -1);
            } else {
                tuples = stringRedisTemplate.opsForZSet().rangeWithScores(shardKey, 0, -1);
            }
            
            if (CollUtil.isNotEmpty(tuples)) {
                allTuples.addAll(tuples);
            }
        }
        
        // 按分数排序
        allTuples.sort((a, b) -> {
            double scoreA = a.getScore() != null ? a.getScore() : 0;
            double scoreB = b.getScore() != null ? b.getScore() : 0;
            return desc ? Double.compare(scoreB, scoreA) : Double.compare(scoreA, scoreB);
        });
        
        // 分页
        int from = (int) start;
        int to = (int) Math.min(end + 1, allTuples.size());
        if (from >= allTuples.size()) {
            return Collections.emptyList();
        }
        
        List<RankingItem> rankingList = new ArrayList<>();
        long rank = start + 1;
        for (int i = from; i < to; i++) {
            ZSetOperations.TypedTuple<String> tuple = allTuples.get(i);
            if (tuple != null && tuple.getValue() != null) {
                double finalScore = tuple.getScore() != null ? tuple.getScore() : 0;
                double originalScore = restoreOriginalScore(finalScore);
                
                RankingItem item = RankingItem.builder()
                        .memberId(tuple.getValue())
                        .score(originalScore)
                        .rank(rank++)
                        .updateTime(System.currentTimeMillis())
                        .build();
                rankingList.add(item);
            }
        }
        
        return rankingList;
    }

    /**
     * 获取排行榜（降序，默认TOP 50）
     * 
     * @param bizType 业务类型
     * @param bizId 业务ID（可选）
     * @return 排行榜列表
     */
    public List<RankingItem> getRanking(String bizType, String bizId) {
        return getRanking(bizType, bizId, 1, DEFAULT_TOP_SIZE, true);
    }

    /**
     * 获取排行榜（分页）
     * 
     * @param bizType 业务类型
     * @param bizId 业务ID（可选）
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @param desc 是否降序（true降序，false升序）
     * @return 排行榜列表
     */
    public List<RankingItem> getRanking(String bizType, String bizId, int page, int size, boolean desc) {
        if (StrUtil.isBlank(bizType) || page < 1 || size < 1) {
            log.warn("获取排行榜参数无效: bizType={}, page={}, size={}", bizType, page, size);
            return Collections.emptyList();
        }

        // 限制分页大小
        size = Math.min(size, MAX_PAGE_SIZE);

        try {
            String baseKey = rankingRedisKeyBuilder.buildRankingKey(bizType, bizId);
            long start = (long) (page - 1) * size;
            long end = start + size - 1;

            // 检查是否已启用分片
            if (isShardingEnabled(baseKey)) {
                // 从分片中获取并合并
                List<RankingItem> rankingList = getRankingFromShards(baseKey, start, end, desc);
                log.debug("获取排行榜(分片): bizType={}, bizId={}, page={}, size={}, desc={}, resultSize={}", 
                        bizType, bizId, page, size, desc, rankingList.size());
                return rankingList;
            } else {
                // 从未分片的key中获取
                Set<ZSetOperations.TypedTuple<String>> tuples;
                if (desc) {
                    // 降序：从高到低
                    tuples = stringRedisTemplate.opsForZSet().reverseRangeWithScores(baseKey, start, end);
                } else {
                    // 升序：从低到高
                    tuples = stringRedisTemplate.opsForZSet().rangeWithScores(baseKey, start, end);
                }

                if (CollUtil.isEmpty(tuples)) {
                    return Collections.emptyList();
                }

                List<RankingItem> rankingList = new ArrayList<>(tuples.size());
                long rank = start + 1;
                for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                    if (tuple != null && tuple.getValue() != null) {
                        double finalScore = tuple.getScore() != null ? tuple.getScore() : 0;
                        // 还原原始分数
                        double originalScore = restoreOriginalScore(finalScore);
                        
                        RankingItem item = RankingItem.builder()
                                .memberId(tuple.getValue())
                                .score(originalScore)
                                .rank(rank++)
                                .updateTime(System.currentTimeMillis())
                                .build();
                        rankingList.add(item);
                    }
                }

                log.debug("获取排行榜: bizType={}, bizId={}, page={}, size={}, desc={}, resultSize={}", 
                        bizType, bizId, page, size, desc, rankingList.size());
                return rankingList;
            }
        } catch (Exception e) {
            log.error("获取排行榜异常: bizType={}, bizId={}, page={}, size={}, desc={}", 
                    bizType, bizId, page, size, desc, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取成员排名（降序排名，从1开始）
     * 
     * @param bizType 业务类型
     * @param bizId 业务ID（可选）
     * @param memberId 成员ID
     * @return 排名，0表示未上榜
     */
    public long getMemberRank(String bizType, String bizId, String memberId) {
        return getMemberRank(bizType, bizId, memberId, true);
    }

    /**
     * 从分片中获取成员排名
     * 
     * @param baseKey 基础key
     * @param memberId 成员ID
     * @param desc 是否降序排名
     * @return 排名，0表示未上榜
     */
    private long getMemberRankFromShards(String baseKey, String memberId, boolean desc) {
        // 先找到成员所在的分片
        int shardIndex = getShardIndex(memberId);
        String shardKey = buildShardKey(baseKey, shardIndex);
        
        // 获取成员分数
        Double memberScore = stringRedisTemplate.opsForZSet().score(shardKey, memberId);
        if (memberScore == null) {
            return 0;
        }
        
        // 获取成员在分片中的排名
        Long shardRank;
        if (desc) {
            shardRank = stringRedisTemplate.opsForZSet().reverseRank(shardKey, memberId);
        } else {
            shardRank = stringRedisTemplate.opsForZSet().rank(shardKey, memberId);
        }
        
        if (shardRank == null) {
            return 0;
        }
        
        // 计算在所有分片中的总排名
        // 需要统计所有分片中分数高于该成员的成员数量
        long rank = 1; // 从1开始
        for (int i = 0; i < SHARD_COUNT; i++) {
            String currentShardKey = buildShardKey(baseKey, i);
            Long count;
            if (desc) {
                // 降序：统计分数大于该成员的
                count = stringRedisTemplate.opsForZSet().count(currentShardKey, memberScore + 0.0001, Double.MAX_VALUE);
            } else {
                // 升序：统计分数小于该成员的
                count = stringRedisTemplate.opsForZSet().count(currentShardKey, Double.MIN_VALUE, memberScore - 0.0001);
            }
            if (count != null) {
                rank += count;
            }
        }
        
        // 加上在当前分片中的排名
        rank += shardRank;
        
        return rank;
    }

    /**
     * 获取成员排名
     * 
     * @param bizType 业务类型
     * @param bizId 业务ID（可选）
     * @param memberId 成员ID
     * @param desc 是否降序排名
     * @return 排名，0表示未上榜
     */
    public long getMemberRank(String bizType, String bizId, String memberId, boolean desc) {
        if (StrUtil.isBlank(bizType) || StrUtil.isBlank(memberId)) {
            log.warn("获取成员排名参数无效: bizType={}, memberId={}", bizType, memberId);
            return 0;
        }

        try {
            String baseKey = rankingRedisKeyBuilder.buildRankingKey(bizType, bizId);
            
            // 检查是否已启用分片
            if (isShardingEnabled(baseKey)) {
                // 从分片中获取排名
                long rank = getMemberRankFromShards(baseKey, memberId, desc);
                log.debug("获取成员排名(分片): bizType={}, bizId={}, memberId={}, desc={}, rank={}", 
                        bizType, bizId, memberId, desc, rank);
                return rank;
            } else {
                // 从未分片的key中获取排名
                Long rank;
                if (desc) {
                    rank = stringRedisTemplate.opsForZSet().reverseRank(baseKey, memberId);
                } else {
                    rank = stringRedisTemplate.opsForZSet().rank(baseKey, memberId);
                }

                if (rank == null) {
                    return 0;
                }

                // Redis排名从0开始，转换为从1开始
                long result = rank + 1;
                log.debug("获取成员排名: bizType={}, bizId={}, memberId={}, desc={}, rank={}", 
                        bizType, bizId, memberId, desc, result);
                return result;
            }
        } catch (Exception e) {
            log.error("获取成员排名异常: bizType={}, bizId={}, memberId={}, desc={}", 
                    bizType, bizId, memberId, desc, e);
            return 0;
        }
    }

    /**
     * 获取成员分数
     * 
     * @param bizType 业务类型
     * @param bizId 业务ID（可选）
     * @param memberId 成员ID
     * @return 分数，null表示成员不存在
     */
    public Double getMemberScore(String bizType, String bizId, String memberId) {
        if (StrUtil.isBlank(bizType) || StrUtil.isBlank(memberId)) {
            log.warn("获取成员分数参数无效: bizType={}, memberId={}", bizType, memberId);
            return null;
        }

        try {
            String baseKey = rankingRedisKeyBuilder.buildRankingKey(bizType, bizId);
            
            // 检查是否已启用分片
            String key;
            if (isShardingEnabled(baseKey)) {
                int shardIndex = getShardIndex(memberId);
                key = buildShardKey(baseKey, shardIndex);
            } else {
                key = baseKey;
            }
            
            Double finalScore = stringRedisTemplate.opsForZSet().score(key, memberId);
            
            if (finalScore == null) {
                return null;
            }

            // 还原原始分数
            double originalScore = restoreOriginalScore(finalScore);
            log.debug("获取成员分数: bizType={}, bizId={}, memberId={}, score={}", 
                    bizType, bizId, memberId, originalScore);
            return originalScore;
        } catch (Exception e) {
            log.error("获取成员分数异常: bizType={}, bizId={}, memberId={}", 
                    bizType, bizId, memberId, e);
            return null;
        }
    }

    /**
     * 获取成员排名信息（包含排名和分数）
     * 
     * @param bizType 业务类型
     * @param bizId 业务ID（可选）
     * @param memberId 成员ID
     * @param desc 是否降序排名
     * @return 排名信息，null表示成员不存在
     */
    public RankingItem getMemberRankingInfo(String bizType, String bizId, String memberId, boolean desc) {
        if (StrUtil.isBlank(bizType) || StrUtil.isBlank(memberId)) {
            log.warn("获取成员排名信息参数无效: bizType={}, memberId={}", bizType, memberId);
            return null;
        }

        try {
            String key = rankingRedisKeyBuilder.buildRankingKey(bizType, bizId);
            
            // 使用Pipeline批量获取排名和分数
            List<Object> results = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                if (desc) {
                    connection.zRevRank(key.getBytes(), memberId.getBytes());
                } else {
                    connection.zRank(key.getBytes(), memberId.getBytes());
                }
                connection.zScore(key.getBytes(), memberId.getBytes());
                return null;
            });

            if (CollUtil.isEmpty(results) || results.size() < 2) {
                return null;
            }

            Long rank = (Long) results.get(0);
            Double finalScore = (Double) results.get(1);

            if (rank == null || finalScore == null) {
                return null;
            }

            // 还原原始分数
            double originalScore = restoreOriginalScore(finalScore);

            RankingItem item = RankingItem.builder()
                    .memberId(memberId)
                    .score(originalScore)
                    .rank(rank + 1L) // Redis排名从0开始，转换为从1开始
                    .updateTime(System.currentTimeMillis())
                    .build();

            log.debug("获取成员排名信息: bizType={}, bizId={}, memberId={}, desc={}, rank={}, score={}", 
                    bizType, bizId, memberId, desc, item.getRank(), item.getScore());
            return item;
        } catch (Exception e) {
            log.error("获取成员排名信息异常: bizType={}, bizId={}, memberId={}, desc={}", 
                    bizType, bizId, memberId, desc, e);
            return null;
        }
    }

    /**
     * 获取排行榜总数
     * 
     * @param bizType 业务类型
     * @param bizId 业务ID（可选）
     * @return 总数
     */
    public long getRankingCount(String bizType, String bizId) {
        if (StrUtil.isBlank(bizType)) {
            log.warn("获取排行榜总数参数无效: bizType={}", bizType);
            return 0;
        }

        try {
            String baseKey = rankingRedisKeyBuilder.buildRankingKey(bizType, bizId);
            
            // 检查是否已启用分片
            if (isShardingEnabled(baseKey)) {
                // 统计所有分片的总数
                long totalCount = 0;
                for (int i = 0; i < SHARD_COUNT; i++) {
                    String shardKey = buildShardKey(baseKey, i);
                    Long count = stringRedisTemplate.opsForZSet().zCard(shardKey);
                    if (count != null) {
                        totalCount += count;
                    }
                }
                log.debug("获取排行榜总数(分片): bizType={}, bizId={}, count={}", bizType, bizId, totalCount);
                return totalCount;
            } else {
                // 从未分片的key中获取
                Long count = stringRedisTemplate.opsForZSet().zCard(baseKey);
                long result = count != null ? count : 0;
                log.debug("获取排行榜总数: bizType={}, bizId={}, count={}", bizType, bizId, result);
                return result;
            }
        } catch (Exception e) {
            log.error("获取排行榜总数异常: bizType={}, bizId={}", bizType, bizId, e);
            return 0;
        }
    }

    /**
     * 删除成员
     * 
     * @param bizType 业务类型
     * @param bizId 业务ID（可选）
     * @param memberId 成员ID
     * @return 是否成功
     */
    public boolean removeMember(String bizType, String bizId, String memberId) {
        if (StrUtil.isBlank(bizType) || StrUtil.isBlank(memberId)) {
            log.warn("删除排行榜成员参数无效: bizType={}, memberId={}", bizType, memberId);
            return false;
        }

        try {
            String key = rankingRedisKeyBuilder.buildRankingKey(bizType, bizId);
            Long removed = stringRedisTemplate.opsForZSet().remove(key, memberId);
            boolean result = removed != null && removed > 0;
            log.debug("删除排行榜成员: bizType={}, bizId={}, memberId={}, result={}", 
                    bizType, bizId, memberId, result);
            return result;
        } catch (Exception e) {
            log.error("删除排行榜成员异常: bizType={}, bizId={}, memberId={}", 
                    bizType, bizId, memberId, e);
            return false;
        }
    }

    /**
     * 批量删除成员
     * 
     * @param bizType 业务类型
     * @param bizId 业务ID（可选）
     * @param memberIds 成员ID列表
     * @return 删除的数量
     */
    public long batchRemoveMembers(String bizType, String bizId, Collection<String> memberIds) {
        if (StrUtil.isBlank(bizType) || CollUtil.isEmpty(memberIds)) {
            log.warn("批量删除排行榜成员参数无效: bizType={}, size={}", bizType, 
                    memberIds != null ? memberIds.size() : 0);
            return 0;
        }

        try {
            String key = rankingRedisKeyBuilder.buildRankingKey(bizType, bizId);
            Long removed = stringRedisTemplate.opsForZSet().remove(key, 
                    memberIds.toArray(new String[0]));
            long result = removed != null ? removed : 0;
            log.debug("批量删除排行榜成员: bizType={}, bizId={}, size={}, removed={}", 
                    bizType, bizId, memberIds.size(), result);
            return result;
        } catch (Exception e) {
            log.error("批量删除排行榜成员异常: bizType={}, bizId={}, size={}", 
                    bizType, bizId, memberIds.size(), e);
            return 0;
        }
    }

    /**
     * 删除整个排行榜
     * 
     * @param bizType 业务类型
     * @param bizId 业务ID（可选）
     * @return 是否成功
     */
    public boolean deleteRanking(String bizType, String bizId) {
        if (StrUtil.isBlank(bizType)) {
            log.warn("删除排行榜参数无效: bizType={}", bizType);
            return false;
        }

        try {
            String key = rankingRedisKeyBuilder.buildRankingKey(bizType, bizId);
            Boolean result = stringRedisTemplate.delete(key);
            log.debug("删除排行榜: bizType={}, bizId={}, result={}", bizType, bizId, result);
            return result;
        } catch (Exception e) {
            log.error("删除排行榜异常: bizType={}, bizId={}", bizType, bizId, e);
            return false;
        }
    }

    /**
     * 设置排行榜过期时间
     * 
     * @param bizType 业务类型
     * @param bizId 业务ID（可选）
     * @param expireSeconds 过期时间（秒）
     * @return 是否成功
     */
    public boolean expireRanking(String bizType, String bizId, long expireSeconds) {
        if (StrUtil.isBlank(bizType) || expireSeconds <= 0) {
            log.warn("设置排行榜过期时间参数无效: bizType={}, expireSeconds={}", bizType, expireSeconds);
            return false;
        }

        try {
            String key = rankingRedisKeyBuilder.buildRankingKey(bizType, bizId);
            Boolean result = stringRedisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
            log.debug("设置排行榜过期时间: bizType={}, bizId={}, expireSeconds={}, result={}", 
                    bizType, bizId, expireSeconds, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("设置排行榜过期时间异常: bizType={}, bizId={}, expireSeconds={}", 
                    bizType, bizId, expireSeconds, e);
            return false;
        }
    }

    /**
     * 获取指定分数区间的成员数量
     * 
     * @param bizType 业务类型
     * @param bizId 业务ID（可选）
     * @param minScore 最小分数
     * @param maxScore 最大分数
     * @return 成员数量
     */
    public long countByScoreRange(String bizType, String bizId, double minScore, double maxScore) {
        if (StrUtil.isBlank(bizType)) {
            log.warn("按分数区间统计参数无效: bizType={}", bizType);
            return 0;
        }

        try {
            String key = rankingRedisKeyBuilder.buildRankingKey(bizType, bizId);
            // 转换为最终分数区间
            // 注意：这里使用原始分数范围，因为时间戳后缀会影响精确匹配
            // 为了准确统计，我们需要考虑时间戳后缀的范围
            double finalMinScore = minScore * SCORE_MULTIPLIER;
            double finalMaxScore = (maxScore + 1) * SCORE_MULTIPLIER - 1;
            
            Long count = stringRedisTemplate.opsForZSet().count(key, finalMinScore, finalMaxScore);
            long result = count != null ? count : 0;
            log.debug("按分数区间统计: bizType={}, bizId={}, minScore={}, maxScore={}, count={}", 
                    bizType, bizId, minScore, maxScore, result);
            return result;
        } catch (Exception e) {
            log.error("按分数区间统计异常: bizType={}, bizId={}, minScore={}, maxScore={}", 
                    bizType, bizId, minScore, maxScore, e);
            return 0;
        }
    }

    /**
     * 获取指定排名区间的成员列表
     * 
     * @param bizType 业务类型
     * @param bizId 业务ID（可选）
     * @param startRank 起始排名（从1开始）
     * @param endRank 结束排名（包含）
     * @param desc 是否降序
     * @return 成员列表
     */
    public List<RankingItem> getRankingByRankRange(String bizType, String bizId, 
            long startRank, long endRank, boolean desc) {
        if (StrUtil.isBlank(bizType) || startRank < 1 || endRank < startRank) {
            log.warn("按排名区间获取参数无效: bizType={}, startRank={}, endRank={}", 
                    bizType, startRank, endRank);
            return Collections.emptyList();
        }

        try {
            String key = rankingRedisKeyBuilder.buildRankingKey(bizType, bizId);
            // Redis排名从0开始
            long start = startRank - 1;
            long end = endRank - 1;

            Set<ZSetOperations.TypedTuple<String>> tuples;
            if (desc) {
                tuples = stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
            } else {
                tuples = stringRedisTemplate.opsForZSet().rangeWithScores(key, start, end);
            }

            if (CollUtil.isEmpty(tuples)) {
                return Collections.emptyList();
            }

            List<RankingItem> rankingList = new ArrayList<>(tuples.size());
            long rank = startRank;
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                if (tuple != null && tuple.getValue() != null) {
                    double finalScore = tuple.getScore() != null ? tuple.getScore() : 0;
                    double originalScore = restoreOriginalScore(finalScore);
                    
                    RankingItem item = RankingItem.builder()
                            .memberId(tuple.getValue())
                            .score(originalScore)
                            .rank(rank++)
                            .updateTime(System.currentTimeMillis())
                            .build();
                    rankingList.add(item);
                }
            }

            log.debug("按排名区间获取: bizType={}, bizId={}, startRank={}, endRank={}, desc={}, resultSize={}", 
                    bizType, bizId, startRank, endRank, desc, rankingList.size());
            return rankingList;
        } catch (Exception e) {
            log.error("按排名区间获取异常: bizType={}, bizId={}, startRank={}, endRank={}, desc={}", 
                    bizType, bizId, startRank, endRank, desc, e);
            return Collections.emptyList();
        }
    }
}
