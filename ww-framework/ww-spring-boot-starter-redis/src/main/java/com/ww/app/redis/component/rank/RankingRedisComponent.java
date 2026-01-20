package com.ww.app.redis.component.rank;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ww.app.redis.component.key.RankingRedisKeyBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
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
     * <p>
     * 注意：当前实现按整数分数设计（如分/积分），小数分数会被截断
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

    /**
     * 分片状态：未启用
     */
    private static final int SHARD_STATUS_DISABLED = 0;

    /**
     * 分片状态：迁移中（双写）
     */
    private static final int SHARD_STATUS_MIGRATING = 2;

    /**
     * 分片状态：已启用（读写分片）
     */
    private static final int SHARD_STATUS_ENABLED = 1;

    /**
     * 分片迁移锁定时间（防止异常导致一直处于迁移中）
     */
    private static final long SHARD_MIGRATING_TTL_MINUTES = 5;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RankingRedisKeyBuilder rankingRedisKeyBuilder;

    /**
     * Lua脚本：原子化增加分数并刷新时间后缀
     */
    private static final String INCREMENT_SCORE_LUA =
            "local key = KEYS[1] \n" +
                    "local member = ARGV[1] \n" +
                    "local increment = tonumber(ARGV[2]) \n" +
                    "local multiplier = tonumber(ARGV[3]) \n" +
                    "local timeAdjust = tonumber(ARGV[4]) \n" +
                    "local current = redis.call('ZSCORE', key, member) \n" +
                    "local currentOriginal = 0 \n" +
                    "if current then \n" +
                    "  currentOriginal = math.floor(tonumber(current) / multiplier) \n" +
                    "end \n" +
                    "local newOriginal = currentOriginal + increment \n" +
                    "local newFinal = newOriginal * multiplier + timeAdjust \n" +
                    "redis.call('ZADD', key, newFinal, member) \n" +
                    "return tostring(newFinal)";

    /**
     * Lua脚本：原子化设置分数并刷新时间后缀
     */
    private static final String SET_SCORE_LUA =
            "local key = KEYS[1] \n" +
                    "local member = ARGV[1] \n" +
                    "local score = tonumber(ARGV[2]) \n" +
                    "local multiplier = tonumber(ARGV[3]) \n" +
                    "local timeAdjust = tonumber(ARGV[4]) \n" +
                    "local finalScore = score * multiplier + timeAdjust \n" +
                    "redis.call('ZADD', key, finalScore, member) \n" +
                    "return tostring(finalScore)";

    /**
     * Lua脚本：原子化增量更新
     */
    private DefaultRedisScript<String> incrementScoreScript;

    /**
     * Lua脚本：原子化设置分数
     */
    private DefaultRedisScript<String> setScoreScript;

    @PostConstruct
    public void init() {
        incrementScoreScript = new DefaultRedisScript<>(INCREMENT_SCORE_LUA, String.class);
        setScoreScript = new DefaultRedisScript<>(SET_SCORE_LUA, String.class);
    }

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
        // 使用floorMod避免hash为Integer.MIN_VALUE时出现负数
        return Math.floorMod(memberId.hashCode(), SHARD_COUNT);
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
        return getShardingStatus(baseKey) == SHARD_STATUS_ENABLED;
    }

    /**
     * 判断是否走分片写入
     *
     * @param shardingStatus 分片状态
     * @return 是否走分片写入
     */
    private boolean isShardWritable(int shardingStatus) {
        return shardingStatus == SHARD_STATUS_ENABLED || shardingStatus == SHARD_STATUS_MIGRATING;
    }

    /**
     * 遍历所有分片并执行处理逻辑
     *
     * @param baseKey 基础key
     * @param shardConsumer 分片处理逻辑
     */
    private void forEachShard(String baseKey, java.util.function.BiConsumer<Integer, String> shardConsumer) {
        for (int i = 0; i < SHARD_COUNT; i++) {
            String shardKey = buildShardKey(baseKey, i);
            shardConsumer.accept(i, shardKey);
        }
    }

    /**
     * 判断是否从分片读取
     *
     * @param shardingStatus 分片状态
     * @return 是否走分片读取
     */
    private boolean isShardReadable(int shardingStatus) {
        // 迁移阶段仍从主key读取，避免历史数据未完全迁移导致读缺失
        return shardingStatus == SHARD_STATUS_ENABLED;
    }

    /**
     * 构建排行条目
     *
     * @param memberId 成员ID
     * @param finalScore Redis中存储的最终分数
     * @param rank 排名（从1开始）
     * @return 排名条目
     */
    private RankingItem buildRankingItem(String memberId, double finalScore, long rank) {
        double originalScore = restoreOriginalScore(finalScore);
        return RankingItem.builder()
                .memberId(memberId)
                .score(originalScore)
                .rank(rank)
                .updateTime(System.currentTimeMillis())
                .build();
    }

    /**
     * 从有序集合结果构建排行榜列表
     *
     * @param tuples ZSet成员列表
     * @param startRank 起始排名（从1开始）
     * @return 排行榜列表
     */
    private List<RankingItem> buildRankingListFromTuples(Set<ZSetOperations.TypedTuple<String>> tuples, long startRank) {
        if (CollUtil.isEmpty(tuples)) {
            return Collections.emptyList();
        }
        List<RankingItem> rankingList = new ArrayList<>(tuples.size());
        long rank = startRank;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple != null && tuple.getValue() != null) {
                double finalScore = tuple.getScore() != null ? tuple.getScore() : 0;
                rankingList.add(buildRankingItem(tuple.getValue(), finalScore, rank++));
            }
        }
        return rankingList;
    }

    /**
     * 根据分片状态获取排行数据
     *
     * @param baseKey 排行榜key
     * @param start 起始索引（从0开始）
     * @param end 结束索引（包含）
     * @param desc 是否降序
     * @return 排行榜列表
     */
    private List<RankingItem> getRankingInternal(String baseKey, long start, long end, boolean desc) {
        int shardingStatus = getShardingStatus(baseKey);
        if (isShardReadable(shardingStatus)) {
            return getRankingFromShards(baseKey, start, end, desc);
        }

        Set<ZSetOperations.TypedTuple<String>> tuples;
        if (desc) {
            tuples = stringRedisTemplate.opsForZSet().reverseRangeWithScores(baseKey, start, end);
        } else {
            tuples = stringRedisTemplate.opsForZSet().rangeWithScores(baseKey, start, end);
        }
        return buildRankingListFromTuples(tuples, start + 1);
    }

    /**
     * 获取分片状态
     */
    private int getShardingStatus(String baseKey) {
        try {
            String flagKey = getShardingFlagKey(baseKey);
            String flag = stringRedisTemplate.opsForValue().get(flagKey);
            if (String.valueOf(SHARD_STATUS_ENABLED).equals(flag)) {
                return SHARD_STATUS_ENABLED;
            }
            if (String.valueOf(SHARD_STATUS_MIGRATING).equals(flag)) {
                return SHARD_STATUS_MIGRATING;
            }
            return SHARD_STATUS_DISABLED;
        } catch (Exception e) {
            log.error("检查分片标记异常: baseKey={}", baseKey, e);
            return SHARD_STATUS_DISABLED;
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
            int status = getShardingStatus(baseKey);
            if (status == SHARD_STATUS_ENABLED) {
                return;
            }
            if (status == SHARD_STATUS_MIGRATING) {
                return;
            }

            // 标记迁移中（双写）
            String flagKey = getShardingFlagKey(baseKey);
            Boolean locked = stringRedisTemplate.opsForValue()
                    .setIfAbsent(flagKey, String.valueOf(SHARD_STATUS_MIGRATING), SHARD_MIGRATING_TTL_MINUTES, TimeUnit.MINUTES);
            if (locked == null || !locked) {
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
            stringRedisTemplate.opsForValue().set(flagKey, String.valueOf(SHARD_STATUS_ENABLED));

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
            // 仅使用时间后缀参与排序，分数仍按原始值计算
            double timeAdjust = calculateFinalScore(0);
            Double finalScore;
            int shardingStatus = getShardingStatus(baseKey);

            // 检查是否已启用分片
            if (shardingStatus == SHARD_STATUS_ENABLED) {
                // 已启用分片，直接写入分片
                int shardIndex = getShardIndex(memberId);
                String shardKey = buildShardKey(baseKey, shardIndex);
                finalScore = executeSetScoreScript(shardKey, memberId, score, timeAdjust);
                log.debug("添加排行榜分数(分片): bizType={}, bizId={}, memberId={}, score={}, shardIndex={}, result={}", 
                        bizType, bizId, memberId, score, shardIndex, finalScore);
                return finalScore != null;
            } else if (shardingStatus == SHARD_STATUS_MIGRATING) {
                // 迁移中，双写主key与分片
                int shardIndex = getShardIndex(memberId);
                String shardKey = buildShardKey(baseKey, shardIndex);
                Double shardScore = executeSetScoreScript(shardKey, memberId, score, timeAdjust);
                Double mainScore = executeSetScoreScript(baseKey, memberId, score, timeAdjust);
                log.debug("添加排行榜分数(迁移双写): bizType={}, bizId={}, memberId={}, score={}, shardIndex={}, result={}",
                        bizType, bizId, memberId, score, shardIndex, shardScore);
                return shardScore != null || mainScore != null;
            } else {
                // 未启用分片，写入主key
                finalScore = executeSetScoreScript(baseKey, memberId, score, timeAdjust);
                
                // 检查是否需要启用分片
                if (needSharding(baseKey)) {
                    // 异步启用分片（避免阻塞）
                    enableSharding(baseKey);
                }
                
                log.debug("添加排行榜分数: bizType={}, bizId={}, memberId={}, score={}, result={}", 
                        bizType, bizId, memberId, score, finalScore);
                return finalScore != null;
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
            int shardingStatus = getShardingStatus(baseKey);
            // 仅使用时间后缀参与排序，分数仍按原始值计算
            double timeAdjust = calculateFinalScore(0);

            // 检查是否已启用分片
            if (shardingStatus == SHARD_STATUS_ENABLED) {
                // 已启用分片，操作分片
                int shardIndex = getShardIndex(memberId);
                String shardKey = buildShardKey(baseKey, shardIndex);
                return executeIncrementScript(shardKey, memberId, increment, timeAdjust);
            } else if (shardingStatus == SHARD_STATUS_MIGRATING) {
                // 迁移中，双写主key与分片
                int shardIndex = getShardIndex(memberId);
                String shardKey = buildShardKey(baseKey, shardIndex);
                Double shardScore = executeIncrementScript(shardKey, memberId, increment, timeAdjust);
                Double mainScore = executeIncrementScript(baseKey, memberId, increment, timeAdjust);
                return shardScore != null ? shardScore : mainScore;
            } else {
                // 未启用分片，原子化增量更新
                Double newScore = executeIncrementScript(baseKey, memberId, increment, timeAdjust);
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
     * 使用Lua脚本原子化更新分数
     */
    private Double executeIncrementScript(String key, String memberId, double increment, double timeAdjust) {
        String result = stringRedisTemplate.execute(
                incrementScoreScript,
                Collections.singletonList(key),
                memberId,
                String.valueOf(increment),
                String.valueOf(SCORE_MULTIPLIER),
                String.valueOf(timeAdjust)
        );
        return result != null ? Double.valueOf(result) : null;
    }

    /**
     * 使用Lua脚本原子化设置分数
     */
    private Double executeSetScoreScript(String key, String memberId, double score, double timeAdjust) {
        String result = stringRedisTemplate.execute(
                setScoreScript,
                Collections.singletonList(key),
                memberId,
                String.valueOf(score),
                String.valueOf(SCORE_MULTIPLIER),
                String.valueOf(timeAdjust)
        );
        return result != null ? Double.valueOf(result) : null;
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
            int shardingStatus = getShardingStatus(baseKey);
            boolean shardingEnabled = shardingStatus == SHARD_STATUS_ENABLED;
            boolean migrating = shardingStatus == SHARD_STATUS_MIGRATING;
            
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
            } else if (migrating) {
                // 迁移中，双写主key与分片
                for (Map.Entry<Integer, Set<ZSetOperations.TypedTuple<String>>> entry : shardMap.entrySet()) {
                    String shardKey = buildShardKey(baseKey, entry.getKey());
                    Set<ZSetOperations.TypedTuple<String>> tuples = entry.getValue();
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

            log.debug("批量添加排行榜分数: bizType={}, bizId={}, size={}, added={}, sharding={}, migrating={}", 
                    bizType, bizId, memberScores.size(), totalAdded, shardingEnabled, migrating);
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
        // 从所有分片中获取当前页所需的最大范围，避免全量拉取
        long rangeEnd = Math.max(end, 0);
        List<ZSetOperations.TypedTuple<String>> allTuples = new ArrayList<>();
        forEachShard(baseKey, (index, shardKey) -> {
            Set<ZSetOperations.TypedTuple<String>> tuples;
            if (desc) {
                tuples = stringRedisTemplate.opsForZSet().reverseRangeWithScores(shardKey, 0, rangeEnd);
            } else {
                tuples = stringRedisTemplate.opsForZSet().rangeWithScores(shardKey, 0, rangeEnd);
            }
            if (CollUtil.isNotEmpty(tuples)) {
                allTuples.addAll(tuples);
            }
        });
        
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
                rankingList.add(buildRankingItem(tuple.getValue(), finalScore, rank++));
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

            List<RankingItem> rankingList = getRankingInternal(baseKey, start, end, desc);
            log.debug("获取排行榜: bizType={}, bizId={}, page={}, size={}, desc={}, resultSize={}", 
                    bizType, bizId, page, size, desc, rankingList.size());
            return rankingList;
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

            if (isShardingEnabled(key)) {
                // 分片情况下，从分片中获取分数与排名
                int shardIndex = getShardIndex(memberId);
                String shardKey = buildShardKey(key, shardIndex);
                Double finalScore = stringRedisTemplate.opsForZSet().score(shardKey, memberId);
                if (finalScore == null) {
                    return null;
                }
                double originalScore = restoreOriginalScore(finalScore);
                long rank = getMemberRankFromShards(key, memberId, desc);
                RankingItem item = RankingItem.builder()
                        .memberId(memberId)
                        .score(originalScore)
                        .rank(rank)
                        .updateTime(System.currentTimeMillis())
                        .build();
                log.debug("获取成员排名信息(分片): bizType={}, bizId={}, memberId={}, desc={}, rank={}, score={}", 
                        bizType, bizId, memberId, desc, item.getRank(), item.getScore());
                return item;
            }
            
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
                long[] totalCount = new long[] {0};
                forEachShard(baseKey, (index, shardKey) -> {
                    Long count = stringRedisTemplate.opsForZSet().zCard(shardKey);
                    if (count != null) {
                        totalCount[0] += count;
                    }
                });
                log.debug("获取排行榜总数(分片): bizType={}, bizId={}, count={}", bizType, bizId, totalCount[0]);
                return totalCount[0];
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
            int shardingStatus = getShardingStatus(key);
            if (shardingStatus == SHARD_STATUS_ENABLED) {
                int shardIndex = getShardIndex(memberId);
                String shardKey = buildShardKey(key, shardIndex);
                Long removed = stringRedisTemplate.opsForZSet().remove(shardKey, memberId);
                boolean result = removed != null && removed > 0;
                log.debug("删除排行榜成员(分片): bizType={}, bizId={}, memberId={}, result={}", 
                        bizType, bizId, memberId, result);
                return result;
            }
            if (shardingStatus == SHARD_STATUS_MIGRATING) {
                int shardIndex = getShardIndex(memberId);
                String shardKey = buildShardKey(key, shardIndex);
                Long removedShard = stringRedisTemplate.opsForZSet().remove(shardKey, memberId);
                Long removedMain = stringRedisTemplate.opsForZSet().remove(key, memberId);
                boolean result = (removedShard != null && removedShard > 0) || (removedMain != null && removedMain > 0);
                log.debug("删除排行榜成员(迁移双删): bizType={}, bizId={}, memberId={}, result={}", 
                        bizType, bizId, memberId, result);
                return result;
            }
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
            int shardingStatus = getShardingStatus(key);
            if (isShardWritable(shardingStatus)) {
                long totalRemoved = 0;
                Map<Integer, List<String>> shardMembers = new HashMap<>();
                for (String memberId : memberIds) {
                    int shardIndex = getShardIndex(memberId);
                    shardMembers.computeIfAbsent(shardIndex, k -> new ArrayList<>()).add(memberId);
                }
                for (Map.Entry<Integer, List<String>> entry : shardMembers.entrySet()) {
                    String shardKey = buildShardKey(key, entry.getKey());
                    Long removed = stringRedisTemplate.opsForZSet().remove(shardKey,
                            entry.getValue().toArray(new String[0]));
                    if (removed != null) {
                        totalRemoved += removed;
                    }
                }
                if (shardingStatus == SHARD_STATUS_MIGRATING) {
                    Long removedMain = stringRedisTemplate.opsForZSet().remove(key,
                            memberIds.toArray(new String[0]));
                    if (removedMain != null) {
                        totalRemoved += removedMain;
                    }
                }
                log.debug("批量删除排行榜成员(分片): bizType={}, bizId={}, size={}, removed={}", 
                        bizType, bizId, memberIds.size(), totalRemoved);
                return totalRemoved;
            }
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
            int shardingStatus = getShardingStatus(key);
            if (shardingStatus != SHARD_STATUS_DISABLED) {
                for (int i = 0; i < SHARD_COUNT; i++) {
                    stringRedisTemplate.delete(buildShardKey(key, i));
                }
                stringRedisTemplate.delete(getShardingFlagKey(key));
            }
            Boolean result = stringRedisTemplate.delete(key);
            log.debug("删除排行榜: bizType={}, bizId={}, result={}", bizType, bizId, result);
            return Boolean.TRUE.equals(result);
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
            int shardingStatus = getShardingStatus(key);
            if (shardingStatus != SHARD_STATUS_DISABLED) {
                for (int i = 0; i < SHARD_COUNT; i++) {
                    stringRedisTemplate.expire(buildShardKey(key, i), expireSeconds, TimeUnit.SECONDS);
                }
                stringRedisTemplate.expire(getShardingFlagKey(key), expireSeconds, TimeUnit.SECONDS);
            }
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

            long result = 0;
            if (isShardingEnabled(key)) {
                long[] totalCount = new long[] {0};
                forEachShard(key, (index, shardKey) -> {
                    Long count = stringRedisTemplate.opsForZSet()
                            .count(shardKey, finalMinScore, finalMaxScore);
                    if (count != null) {
                        totalCount[0] += count;
                    }
                });
                result = totalCount[0];
            } else {
                Long count = stringRedisTemplate.opsForZSet().count(key, finalMinScore, finalMaxScore);
                result = count != null ? count : 0;
            }
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

            List<RankingItem> rankingList = getRankingInternal(key, start, end, desc);
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
