package com.ww.app.redis.component;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ww.app.common.exception.ApiException;
import com.ww.app.redis.component.key.LikeRedisKeyBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author ww
 * @create 2024-12-21 11:04
 * @description: 点赞Redis组件
 * 支持高性能、高并发、高可用性的点赞功能
 * 采用多种数据结构组合实现，满足不同场景的需求
 * 针对大规模点赞进行分片优化，避免bigkey问题
 */
@Slf4j
@Component
public class LikeRedisComponent {

    /**
     * 常量类，定义各种配置和错误消息
     */
    public static class LikeConstants {
        /**
         * 内容类型枚举
         */
        public static class ContentType {
            public static final String POST = "post";       // 帖子
            public static final String COMMENT = "comment"; // 评论
            public static final String ARTICLE = "article"; // 文章
            public static final String VIDEO = "video";     // 视频
        }

        /**
         * 错误消息
         */
        public static class ErrorMessage {
            public static final String EMPTY_PARAMS = "参数不能为空";
            public static final String INVALID_PARAMS = "参数错误";
            public static final String TIME_RANGE_ERROR = "开始时间不能大于结束时间";
            public static final String ADD_LIKE_FAILED = "添加点赞失败";
            public static final String REMOVE_LIKE_FAILED = "取消点赞失败";
            public static final String CHECK_LIKE_STATUS_FAILED = "检查点赞状态失败";
            public static final String GET_LIKE_COUNT_FAILED = "获取点赞数失败";
            public static final String GET_USER_LIKES_FAILED = "获取用户点赞内容失败";
            public static final String GET_CONTENT_LIKED_USERS_FAILED = "获取内容点赞用户失败";
            public static final String GET_HOT_RANKING_FAILED = "获取热门内容排行榜失败";
            public static final String CLEAN_EXPIRED_DATA_FAILED = "清理过期点赞数据失败";
            public static final String GET_LIKE_STATUS_FAILED = "批量获取点赞状态失败";
            public static final String GET_LIKE_TYPE_DISTRIBUTION_FAILED = "获取用户点赞类型分布失败";
            public static final String UPDATE_HOT_SCORE_FAILED = "更新内容热度分数失败";
            public static final String RESET_LIKE_STATUS_FAILED = "重置内容点赞状态失败";
            public static final String GET_COMMON_LIKES_FAILED = "获取共同点赞内容数量失败";
        }
    }

    /**
     * 点赞操作异常
     * 由于ApiException没有接受Throwable的构造函数，我们需要修改异常处理逻辑
     */
    public static class LikeOperationException extends ApiException {
        public LikeOperationException(String message) {
            super(message);
        }
        
        // 此构造函数在调用处需要修改使用方式
    }

    /**
     * 默认过期时间（天）
     */
    @Value("${app.like.expire-days:30}")
    private int defaultExpireDays;

    /**
     * 批处理数量
     */
    @Value("${app.like.batch-size:1000}")
    private int batchSize;

    /**
     * 用于分片的桶数量
     * 将用户分散存储到多个子SET中，避免bigkey
     */
    @Value("${app.like.sharding.bucket-count:64}")
    private int shardingBucketCount;

    /**
     * 点赞用户数超过此阈值时启用分片存储
     */
    @Value("${app.like.sharding.threshold:10000}")
    private int shardingThreshold;

    /**
     * Lua脚本：添加点赞，保证原子性操作（支持分片）
     */
    private static final String ADD_LIKE_LUA =
            "local userKey = KEYS[1] \n" +                          // 用户点赞集合
                    "local contentKey = KEYS[2] \n" +                       // 内容点赞集合
                    "local countKey = KEYS[3] \n" +                         // 点赞计数
                    "local rankKey = KEYS[4] \n" +                          // 热门排行榜
                    "local hllKey = KEYS[5] \n" +                           // HyperLogLog计数
                    "local bfKey = KEYS[6] \n" +                            // 布隆过滤器
                    "local shardingKey = KEYS[7] \n" +                      // 分片信息键
                    "local userId = ARGV[1] \n" +                           // 用户ID
                    "local contentId = ARGV[2] \n" +                        // 内容ID
                    "local expireDays = tonumber(ARGV[3]) \n" +             // 过期时间(天)
                    "local shardingThreshold = tonumber(ARGV[4]) \n" +      // 分片阈值
                    "local shardBucketCount = tonumber(ARGV[5]) \n" +       // 分片桶数量
                    "local shardId = tonumber(ARGV[6]) \n" +                // 分片ID

                    "-- 先检查用户是否已点赞 \n" +
                    "local isNewLike = redis.call('SADD', userKey, contentId) \n" +
                    "if isNewLike == 0 then \n" +
                    "   return 0 \n" +                                      // 已经点赞过
                    "end \n" +

                    "-- 检查是否需要分片 \n" +
                    "local isSharding = redis.call('GET', shardingKey) \n" +
                    "if isSharding == '1' then \n" +
                    "   -- 已经分片，直接使用分片key添加用户ID \n" +
                    "   redis.call('SADD', contentKey, userId) \n" +
                    "else \n" +
                    "   -- 添加到普通内容点赞集合 \n" +
                    "   redis.call('SADD', contentKey, userId) \n" +
                    "   -- 检查是否需要开始分片 \n" +
                    "   local likeCount = redis.call('SCARD', contentKey) \n" +
                    "   if likeCount >= shardingThreshold then \n" +
                    "       -- 启用分片，将现有数据迁移到分片中 \n" +
                    "       local allUsers = redis.call('SMEMBERS', contentKey) \n" +
                    "       for i, uid in ipairs(allUsers) do \n" +
                    "           local userShardId = redis.call('CRC32', uid) % shardBucketCount \n" +
                    "           local shardKey = contentKey .. ':' .. userShardId \n" +
                    "           redis.call('SADD', shardKey, uid) \n" +
                    "           redis.call('EXPIRE', shardKey, expireDays * 86400) \n" +
                    "       end \n" +
                    "       -- 标记为已分片 \n" +
                    "       redis.call('SET', shardingKey, '1') \n" +
                    "       redis.call('EXPIRE', shardingKey, expireDays * 86400) \n" +
                    "   end \n" +
                    "end \n" +

                    "-- 更新HyperLogLog计数 \n" +
                    "redis.call('PFADD', hllKey, userId) \n" +

                    "-- 更新点赞计数 \n" +
                    "local count = redis.call('INCR', countKey) \n" +

                    "-- 更新排行榜 \n" +
                    "redis.call('ZADD', rankKey, count, contentId) \n" +

                    "-- 设置过期时间 \n" +
                    "redis.call('EXPIRE', userKey, expireDays * 86400) \n" +
                    "redis.call('EXPIRE', contentKey, expireDays * 86400) \n" +
                    "redis.call('EXPIRE', countKey, expireDays * 86400) \n" +
                    "redis.call('EXPIRE', rankKey, expireDays * 86400) \n" +
                    "redis.call('EXPIRE', hllKey, expireDays * 86400) \n" +
                    "redis.call('EXPIRE', bfKey, expireDays * 86400) \n" +

                    "return 1";                                      // 点赞成功

    /**
     * Lua脚本：取消点赞，保证原子性操作
     */
    private static final String REMOVE_LIKE_LUA =
            "local userKey = KEYS[1] \n" +                          // 用户点赞集合
                    "local contentKey = KEYS[2] \n" +                       // 内容点赞集合
                    "local countKey = KEYS[3] \n" +                         // 点赞计数
                    "local rankKey = KEYS[4] \n" +                          // 热门排行榜
                    "local shardingKey = KEYS[5] \n" +                      // 分片信息键
                    "local userId = ARGV[1] \n" +                           // 用户ID
                    "local contentId = ARGV[2] \n" +                        // 内容ID
                    "local shardId = tonumber(ARGV[3]) \n" +                // 分片ID

                    "-- 从用户点赞集合移除 \n" +
                    "local isRemoved = redis.call('SREM', userKey, contentId) \n" +
                    "if isRemoved == 0 then \n" +
                    "   return 0 \n" +                                      // 未点赞过
                    "end \n" +

                    "-- 检查是否分片 \n" +
                    "local isSharding = redis.call('GET', shardingKey) \n" +
                    "if isSharding == '1' then \n" +
                    "   -- 已分片，从对应分片中移除 \n" +
                    "   local shardKey = contentKey .. ':' .. shardId \n" +
                    "   redis.call('SREM', shardKey, userId) \n" +
                    "else \n" +
                    "   -- 未分片，直接从内容点赞集合移除 \n" +
                    "   redis.call('SREM', contentKey, userId) \n" +
                    "end \n" +

                    "-- 更新点赞计数 \n" +
                    "local count = redis.call('DECR', countKey) \n" +
                    "if count <= 0 then \n" +
                    "   redis.call('DEL', countKey) \n" +
                    "   redis.call('ZREM', rankKey, contentId) \n" +
                    "else \n" +
                    "   redis.call('ZADD', rankKey, count, contentId) \n" +
                    "end \n" +

                    "return 1";                                      // 取消点赞成功

    /**
     * Lua脚本：批量获取点赞状态
     */
    private static final String BATCH_LIKE_STATUS_LUA =
            "local userKey = KEYS[1] \n" +                          // 用户点赞集合
                    "local result = {} \n" +
                    "for i=1, #ARGV do \n" +
                    "   local contentId = ARGV[i] \n" +
                    "   result[i] = redis.call('SISMEMBER', userKey, contentId) \n" + // 检查是否点赞
                    "end \n" +
                    "return result";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private LikeRedisKeyBuilder likeRedisKeyBuilder;

    private DefaultRedisScript<Long> addLikeScript;
    private DefaultRedisScript<Long> removeLikeScript;
    private DefaultRedisScript<List> batchLikeStatusScript;

    @PostConstruct
    public void init() {
        // 初始化Lua脚本
        addLikeScript = new DefaultRedisScript<>(ADD_LIKE_LUA, Long.class);
        removeLikeScript = new DefaultRedisScript<>(REMOVE_LIKE_LUA, Long.class);
        batchLikeStatusScript = new DefaultRedisScript<>(BATCH_LIKE_STATUS_LUA, List.class);

        log.info("点赞Redis组件初始化完成，已启用分片优化，分片阈值={}, 分片桶数量={}, 批处理大小={}, 默认过期天数={}",
                shardingThreshold, shardingBucketCount, batchSize, defaultExpireDays);
    }

    /**
     * 计算用户ID的分片ID
     */
    private int calculateShardId(String userId) {
        return Math.abs(userId.hashCode()) % shardingBucketCount;
    }

    /**
     * 检查内容是否已启用分片
     */
    private boolean isContentSharded(String contentType, String contentId) {
        String shardingKey = likeRedisKeyBuilder.buildShardingKey(contentType, contentId);
        String value = stringRedisTemplate.opsForValue().get(shardingKey);
        return "1".equals(value);
    }

    /**
     * 添加点赞
     *
     * @param userId      用户ID
     * @param contentType 内容类型
     * @param contentId   内容ID
     * @return 是否成功添加点赞
     */
    public boolean addLike(String userId, String contentType, String contentId) {
        return addLike(userId, contentType, contentId, defaultExpireDays);
    }

    /**
     * 添加点赞（带过期时间）
     *
     * @param userId      用户ID
     * @param contentType 内容类型
     * @param contentId   内容ID
     * @param expireDays  过期天数
     * @return 是否成功添加点赞
     */
    public boolean addLike(String userId, String contentType, String contentId, int expireDays) {
        validateBasicParams(userId, contentType, contentId);

        try {
            // 计算分片ID
            int shardId = calculateShardId(userId);

            // 准备所有需要的key
            String userLikeKey = likeRedisKeyBuilder.buildUserLikeKey(userId);
            String contentLikeKey = likeRedisKeyBuilder.buildContentLikeKey(contentType, contentId, null); // 基础key，不带分片
            String likeCountKey = likeRedisKeyBuilder.buildCountKey(contentType, contentId);
            String likeRankKey = likeRedisKeyBuilder.buildRankKey(contentType);
            String likeHllKey = likeRedisKeyBuilder.buildHllKey(contentType, contentId);
            String likeBfKey = likeRedisKeyBuilder.buildBfKey(contentType, contentId);
            String shardingKey = likeRedisKeyBuilder.buildShardingKey(contentType, contentId);

            // 执行Lua脚本，保证原子性操作
            Long result = stringRedisTemplate.execute(
                    addLikeScript,
                    Arrays.asList(userLikeKey, contentLikeKey, likeCountKey, likeRankKey, likeHllKey, likeBfKey, shardingKey),
                    userId, contentId, String.valueOf(expireDays),
                    String.valueOf(shardingThreshold), String.valueOf(shardingBucketCount), String.valueOf(shardId)
            );

            return result == 1;
        } catch (Exception e) {
            log.error("添加点赞异常: userId={}, contentType={}, contentId={}", userId, contentType, contentId, e);
            // 由于没有接受Throwable的构造函数，我们只使用错误消息
            throw new LikeOperationException(LikeConstants.ErrorMessage.ADD_LIKE_FAILED);
        }
    }

    /**
     * 取消点赞
     *
     * @param userId      用户ID
     * @param contentType 内容类型
     * @param contentId   内容ID
     * @return 是否成功取消点赞
     */
    public boolean removeLike(String userId, String contentType, String contentId) {
        validateBasicParams(userId, contentType, contentId);

        try {
            // 计算分片ID
            int shardId = calculateShardId(userId);

            String userLikeKey = likeRedisKeyBuilder.buildUserLikeKey(userId);
            String contentLikeKey = likeRedisKeyBuilder.buildContentLikeKey(contentType, contentId, null); // 基础key，不带分片
            String likeCountKey = likeRedisKeyBuilder.buildCountKey(contentType, contentId);
            String likeRankKey = likeRedisKeyBuilder.buildRankKey(contentType);
            String shardingKey = likeRedisKeyBuilder.buildShardingKey(contentType, contentId);

            // 执行Lua脚本，保证原子性操作
            Long result = stringRedisTemplate.execute(
                    removeLikeScript,
                    Arrays.asList(userLikeKey, contentLikeKey, likeCountKey, likeRankKey, shardingKey),
                    userId, contentId, String.valueOf(shardId)
            );

            return result == 1;
        } catch (Exception e) {
            log.error("取消点赞异常: userId={}, contentType={}, contentId={}", userId, contentType, contentId, e);
            throw new LikeOperationException(LikeConstants.ErrorMessage.REMOVE_LIKE_FAILED);
        }
    }

    /**
     * 检查用户是否已点赞指定内容
     *
     * @param userId      用户ID
     * @param contentType 内容类型
     * @param contentId   内容ID
     * @return 是否已点赞
     */
    public boolean hasLiked(String userId, String contentType, String contentId) {
        validateBasicParams(userId, contentType, contentId);

        try {
            // 使用用户的点赞集合判断
            String userLikeKey = likeRedisKeyBuilder.buildUserLikeKey(userId);
            return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(userLikeKey, contentId));
        } catch (Exception e) {
            log.error("检查点赞状态异常: userId={}, contentType={}, contentId={}", userId, contentType, contentId, e);
            throw new LikeOperationException(LikeConstants.ErrorMessage.CHECK_LIKE_STATUS_FAILED);
        }
    }

    /**
     * 批量检查用户是否已点赞多个内容
     *
     * @param userId     用户ID
     * @param contentIds 内容ID列表
     * @return 点赞状态结果列表，顺序与输入的contentIds一致
     */
    public List<Boolean> batchHasLiked(String userId, List<String> contentIds) {
        if (StrUtil.isBlank(userId) || CollUtil.isEmpty(contentIds)) {
            throw new LikeOperationException(LikeConstants.ErrorMessage.EMPTY_PARAMS);
        }

        try {
            String userLikeKey = likeRedisKeyBuilder.buildUserLikeKey(userId);

            // 执行Lua脚本，批量获取点赞状态
            List<Long> results = stringRedisTemplate.execute(
                    batchLikeStatusScript,
                    Collections.singletonList(userLikeKey),
                    contentIds.toArray(new String[0])
            );

            // 转换结果
            return results.stream()
                    .map(result -> result == 1)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("批量检查点赞状态异常: userId={}, contentIds数量={}", userId, contentIds.size(), e);
            throw new LikeOperationException(LikeConstants.ErrorMessage.CHECK_LIKE_STATUS_FAILED);
        }
    }

    /**
     * 获取内容的点赞数
     * 对于大规模点赞内容，可以提供近似值选项
     *
     * @param contentType 内容类型
     * @param contentId   内容ID
     * @param approximate 是否使用近似值（使用HyperLogLog）
     * @return 点赞数
     */
    public long getLikeCount(String contentType, String contentId, boolean approximate) {
        if (StrUtil.hasBlank(contentType, contentId)) {
            throw new LikeOperationException(LikeConstants.ErrorMessage.EMPTY_PARAMS);
        }

        try {
            // 如果需要精确值或未分片，直接返回计数器值
            if (!approximate || !isContentSharded(contentType, contentId)) {
                String likeCountKey = likeRedisKeyBuilder.buildCountKey(contentType, contentId);
                String count = stringRedisTemplate.opsForValue().get(likeCountKey);
                return count != null ? Long.parseLong(count) : 0;
            } else {
                // 使用HyperLogLog估算点赞数（对于大规模点赞的内容）
                String likeHllKey = likeRedisKeyBuilder.buildHllKey(contentType, contentId);
                Long count = stringRedisTemplate.execute((RedisCallback<Long>) connection ->
                        connection.pfCount(likeHllKey.getBytes(StandardCharsets.UTF_8)));
                return count != null ? count : 0;
            }
        } catch (Exception e) {
            log.error("获取点赞数异常: contentType={}, contentId={}", contentType, contentId, e);
            throw new LikeOperationException(LikeConstants.ErrorMessage.GET_LIKE_COUNT_FAILED);
        }
    }

    /**
     * 获取内容的点赞数（默认精确值）
     */
    public long getLikeCount(String contentType, String contentId) {
        return getLikeCount(contentType, contentId, false);
    }

    /**
     * 批量获取多个内容的点赞数
     *
     * @param contentType 内容类型
     * @param contentIds  内容ID列表
     * @return 内容点赞数映射，key为contentId，value为点赞数
     */
    public Map<String, Long> batchGetLikeCount(String contentType, List<String> contentIds) {
        if (StrUtil.isBlank(contentType) || CollUtil.isEmpty(contentIds)) {
            throw new LikeOperationException(LikeConstants.ErrorMessage.EMPTY_PARAMS);
        }

        try {
            // 构建需要查询的key列表
            List<String> countKeys = contentIds.stream()
                    .map(contentId -> likeRedisKeyBuilder.buildCountKey(contentType, contentId))
                    .collect(Collectors.toList());

            // 批量获取点赞数
            List<String> countValues = stringRedisTemplate.opsForValue().multiGet(countKeys);

            // 构建结果映射
            Map<String, Long> result = new HashMap<>(contentIds.size());
            for (int i = 0; i < contentIds.size(); i++) {
                String count = countValues != null && i < countValues.size() ? countValues.get(i) : null;
                result.put(contentIds.get(i), count != null ? Long.parseLong(count) : 0);
            }

            return result;
        } catch (Exception e) {
            log.error("批量获取点赞数异常: contentType={}, contentIds数量={}", contentType, contentIds.size(), e);
            throw new LikeOperationException(LikeConstants.ErrorMessage.GET_LIKE_COUNT_FAILED);
        }
    }

    /**
     * 获取点赞了特定内容的所有用户ID
     * 注意：对于大规模点赞的内容，这个操作可能很耗资源，应谨慎使用
     *
     * @param contentType 内容类型
     * @param contentId   内容ID
     * @param limit       返回的最大用户数量，设为负数则不限制
     * @return 点赞用户ID列表
     */
    public Set<String> getContentLikedUsers(String contentType, String contentId, int limit) {
        if (StrUtil.hasBlank(contentType, contentId)) {
            throw new LikeOperationException(LikeConstants.ErrorMessage.EMPTY_PARAMS);
        }

        try {
            // 检查是否分片
            boolean isSharded = isContentSharded(contentType, contentId);

            if (!isSharded) {
                // 未分片，直接获取
                String contentLikeKey = likeRedisKeyBuilder.buildContentLikeKey(contentType, contentId, null);
                if (limit > 0) {
                    // 有数量限制，使用SRANDMEMBER
                    return new HashSet<>(Objects.requireNonNull(stringRedisTemplate.opsForSet().randomMembers(contentLikeKey, limit)));
                } else {
                    // 无数量限制，获取全部
                    Set<String> userIds = stringRedisTemplate.opsForSet().members(contentLikeKey);
                    return userIds != null ? userIds : new HashSet<>();
                }
            } else {
                // 已分片，需要从多个分片获取并合并
                Set<String> result = new HashSet<>();

                // 限制总查询量，防止资源消耗过大
                int actualLimit = limit > 0 ? limit : 10000;
                int remainingLimit = actualLimit;

                // 随机选择几个分片获取用户
                List<Integer> shardIds = new ArrayList<>();
                for (int i = 0; i < shardingBucketCount; i++) {
                    shardIds.add(i);
                }
                Collections.shuffle(shardIds);

                for (int shardId : shardIds) {
                    if (remainingLimit <= 0) break;

                    String shardKey = likeRedisKeyBuilder.buildContentLikeKey(contentType, contentId, shardId);
                    Set<String> shardUsers;

                    if (remainingLimit < actualLimit) {
                        // 部分获取
                        shardUsers = new HashSet<>(Objects.requireNonNull(stringRedisTemplate.opsForSet().randomMembers(shardKey, remainingLimit)));
                    } else {
                        // 获取该分片全部用户
                        shardUsers = stringRedisTemplate.opsForSet().members(shardKey);
                    }

                    if (shardUsers != null && !shardUsers.isEmpty()) {
                        result.addAll(shardUsers);
                        remainingLimit -= shardUsers.size();
                    }
                }

                return result;
            }
        } catch (Exception e) {
            log.error("获取内容点赞用户异常: contentType={}, contentId={}, limit={}", contentType, contentId, limit, e);
            throw new LikeOperationException(LikeConstants.ErrorMessage.GET_CONTENT_LIKED_USERS_FAILED);
        }
    }

    /**
     * 获取点赞了特定内容的所有用户ID（不限制数量）
     */
    public Set<String> getContentLikedUsers(String contentType, String contentId) {
        return getContentLikedUsers(contentType, contentId, -1);
    }

    /**
     * 获取用户点赞的所有内容ID
     *
     * @param userId 用户ID
     * @return 点赞的内容ID列表
     */
    public Set<String> getUserLikedContent(String userId) {
        if (StrUtil.isBlank(userId)) {
            throw new LikeOperationException(LikeConstants.ErrorMessage.EMPTY_PARAMS);
        }

        try {
            String userLikeKey = likeRedisKeyBuilder.buildUserLikeKey(userId);
            Set<String> contentIds = stringRedisTemplate.opsForSet().members(userLikeKey);
            return contentIds != null ? contentIds : new HashSet<>();
        } catch (Exception e) {
            log.error("获取用户点赞内容异常: userId={}", userId, e);
            throw new LikeOperationException(LikeConstants.ErrorMessage.GET_USER_LIKES_FAILED);
        }
    }

    /**
     * 获取热门内容排行榜
     *
     * @param contentType 内容类型
     * @param limit       返回数量限制
     * @return 排行榜内容ID和点赞数Map，按点赞数从高到低排序
     */
    public List<Map<String, Object>> getHotContentRanking(String contentType, int limit) {
        if (StrUtil.isBlank(contentType) || limit <= 0) {
            throw new LikeOperationException(LikeConstants.ErrorMessage.INVALID_PARAMS);
        }

        try {
            String rankKey = likeRedisKeyBuilder.buildRankKey(contentType);
            Set<ZSetOperations.TypedTuple<String>> typedTuples =
                    stringRedisTemplate.opsForZSet().reverseRangeWithScores(rankKey, 0, limit - 1);

            if (CollUtil.isEmpty(typedTuples)) {
                return new ArrayList<>();
            }

            // 转换结果格式
            List<Map<String, Object>> result = new ArrayList<>(typedTuples.size());
            for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
                Map<String, Object> item = new HashMap<>(2);
                item.put("contentId", tuple.getValue());
                item.put("likeCount", tuple.getScore() != null ? tuple.getScore().longValue() : 0);
                result.add(item);
            }

            return result;
        } catch (Exception e) {
            log.error("获取热门内容排行榜异常: contentType={}, limit={}", contentType, limit, e);
            throw new LikeOperationException(LikeConstants.ErrorMessage.GET_HOT_RANKING_FAILED);
        }
    }

    /**
     * 清理过期的点赞数据
     *
     * @param contentType 内容类型
     * @param days        保留最近几天的数据
     * @return 清理的key数量
     */
    public long cleanExpiredLikeData(String contentType, int days) {
        if (StrUtil.isBlank(contentType) || days <= 0) {
            throw new LikeOperationException(LikeConstants.ErrorMessage.INVALID_PARAMS);
        }

        try {
            // 构建模式查询的前缀
            String countKeyPrefix = likeRedisKeyBuilder.buildCountKey(contentType, "");
            String pattern = countKeyPrefix.substring(0, countKeyPrefix.lastIndexOf(":") + 1) + "*";

            // 获取需要清理的点赞计数key
            Set<String> expiredCountKeys = getKeysWithoutExpiry(pattern);
            
            if (CollUtil.isEmpty(expiredCountKeys)) {
                return 0;
            }

            // 设置过期时间
            long count = 0;
            for (String countKey : expiredCountKeys) {
                // 从countKey中提取contentId
                String[] parts = countKey.split(":");
                if (parts.length < 4) { // 格式应为 app:like:count:contentType:contentId
                    continue;
                }

                String contentId = parts[parts.length - 1];
                String contentLikeKey = likeRedisKeyBuilder.buildContentLikeKey(contentType, contentId, null);

                // 设置过期时间
                Boolean result1 = stringRedisTemplate.expire(countKey, days, TimeUnit.DAYS);
                Boolean result2 = stringRedisTemplate.expire(contentLikeKey, days, TimeUnit.DAYS);

                if (Boolean.TRUE.equals(result1) || Boolean.TRUE.equals(result2)) {
                    count++;
                }
            }

            // 对排行榜也设置过期时间
            stringRedisTemplate.expire(likeRedisKeyBuilder.buildRankKey(contentType), days, TimeUnit.DAYS);

            return count;
        } catch (Exception e) {
            log.error("清理过期点赞数据异常: contentType={}, days={}", contentType, days, e);
            throw new LikeOperationException(LikeConstants.ErrorMessage.CLEAN_EXPIRED_DATA_FAILED);
        }
    }

    /**
     * 获取没有设置过期时间的键
     * 
     * @param pattern 匹配模式
     * @return 未设置过期时间的键集合
     */
    private Set<String> getKeysWithoutExpiry(String pattern) {
        return stringRedisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keySet = new HashSet<>();
            try {
                ScanOptions options = ScanOptions.scanOptions().match(pattern).count(batchSize).build();
                Cursor<byte[]> cursor = connection.scan(options);
                
                while (cursor.hasNext()) {
                    byte[] keyBytes = cursor.next();
                    String key = new String(keyBytes, StandardCharsets.UTF_8);
                    // 检查key是否已设置过期时间
                    Long ttl = connection.ttl(keyBytes);
                    if (ttl != null && ttl == -1) { // -1表示永不过期
                        keySet.add(key);
                    }
                }
                cursor.close();
            } catch (Exception e) {
                log.warn("扫描Redis键异常", e);
            }
            return keySet;
        });
    }

    /**
     * 批量获取多个内容的点赞状态
     *
     * @param userId      用户ID
     * @param contentType 内容类型（用于日志记录）
     * @param contentIds  内容ID列表
     * @return 点赞状态映射，key为contentId，value为是否已点赞
     */
    public Map<String, Boolean> batchGetLikeStatus(String userId, String contentType, List<String> contentIds) {
        if (StrUtil.isBlank(userId) || CollUtil.isEmpty(contentIds)) {
            throw new LikeOperationException(LikeConstants.ErrorMessage.EMPTY_PARAMS);
        }

        try {
            // 批量检查点赞状态
            List<Boolean> likeStatus = batchHasLiked(userId, contentIds);
            
            // 构建结果映射
            Map<String, Boolean> result = new HashMap<>(contentIds.size());
            for (int i = 0; i < contentIds.size(); i++) {
                result.put(contentIds.get(i), i < likeStatus.size() && likeStatus.get(i));
            }
            
            return result;
        } catch (Exception e) {
            log.error("批量获取点赞状态异常: userId={}, contentType={}, contentIds数量={}", 
                    userId, contentType, contentIds.size(), e);
            throw new LikeOperationException(LikeConstants.ErrorMessage.GET_LIKE_STATUS_FAILED);
        }
    }

    /**
     * 获取指定内容类型的总点赞数（所有内容的点赞数总和）
     *
     * @param contentType 内容类型
     * @return 总点赞数
     */
    public long getTotalLikeCount(String contentType) {
        if (StrUtil.isBlank(contentType)) {
            throw new LikeOperationException(LikeConstants.ErrorMessage.EMPTY_PARAMS);
        }

        try {
            String pattern = likeRedisKeyBuilder.buildCountKey(contentType, "*");

            // 使用scan命令分批获取所有点赞计数键值，避免使用keys命令阻塞Redis
            final AtomicLong totalCount = new AtomicLong(0);
            
            // 使用pipeline批量获取，提高性能
            iterateByPattern(pattern, keys -> {
                if (CollUtil.isNotEmpty(keys)) {
                    List<Object> countValues = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                        for (String key : keys) {
                            connection.get(key.getBytes(StandardCharsets.UTF_8));
                        }
                        return null;
                    });
                    
                    // 累加所有值
                    for (Object countObj : countValues) {
                        if (countObj != null) {
                            try {
                                totalCount.addAndGet(Long.parseLong(countObj.toString()));
                            } catch (NumberFormatException ignored) {
                                // 忽略非数字值
                            }
                        }
                    }
                }
            });
            
            return totalCount.get();
        } catch (Exception e) {
            log.error("获取总点赞数异常: contentType={}", contentType, e);
            throw new LikeOperationException(LikeConstants.ErrorMessage.GET_LIKE_COUNT_FAILED);
        }
    }

    /**
     * 使用pattern遍历键，并分批处理
     * 
     * @param pattern 匹配模式
     * @param consumer 每批键的处理函数
     */
    private void iterateByPattern(String pattern, Consumer<Set<String>> consumer) {
        stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
            try {
                ScanOptions options = ScanOptions.scanOptions().match(pattern).count(batchSize).build();
                Cursor<byte[]> cursor = connection.scan(options);
                
                Set<String> batch = new HashSet<>(batchSize);
                while (cursor.hasNext()) {
                    byte[] keyBytes = cursor.next();
                    batch.add(new String(keyBytes, StandardCharsets.UTF_8));
                    
                    // 达到批处理大小就处理一次
                    if (batch.size() >= batchSize) {
                        consumer.accept(batch);
                        batch = new HashSet<>(batchSize);
                    }
                }
                
                // 处理最后一批
                if (!batch.isEmpty()) {
                    consumer.accept(batch);
                }
                
                cursor.close();
            } catch (Exception e) {
                log.warn("遍历键异常: pattern={}", pattern, e);
            }
            return null;
        });
    }

    /**
     * 获取用户在某个时间段内点赞的内容
     * 需要与时间序列存储结合使用，此处为示例实现
     *
     * @param userId      用户ID
     * @param contentType 内容类型
     * @param startTime   开始时间（毫秒时间戳）
     * @param endTime     结束时间（毫秒时间戳）
     * @return 时间范围内点赞的内容ID列表
     */
    public Set<String> getUserLikedContentInTimeRange(String userId, String contentType, long startTime, long endTime) {
        if (StrUtil.isBlank(userId) || StrUtil.isBlank(contentType)) {
            throw new LikeOperationException(LikeConstants.ErrorMessage.EMPTY_PARAMS);
        }

        if (startTime > endTime) {
            throw new LikeOperationException(LikeConstants.ErrorMessage.TIME_RANGE_ERROR);
        }

        // 注意：此方法需要与时间序列存储结合，这里仅返回用户所有点赞内容作为示例
        // 实际实现可考虑使用Redis时间序列模块或其他时间序列数据库
        log.warn("获取时间范围内点赞内容功能需与时间序列存储结合，目前返回用户所有点赞内容");

        return getUserLikedContent(userId);
    }

    /**
     * 统计用户点赞的内容类型分布
     *
     * @param userId       用户ID
     * @param contentTypes 内容类型列表
     * @return 各内容类型的点赞数量映射
     */
    public Map<String, Long> getUserLikeTypeDistribution(String userId, List<String> contentTypes) {
        if (StrUtil.isBlank(userId) || CollUtil.isEmpty(contentTypes)) {
            throw new LikeOperationException(LikeConstants.ErrorMessage.EMPTY_PARAMS);
        }

        try {
            Map<String, Long> result = new HashMap<>(contentTypes.size());

            // 获取用户点赞的所有内容
            Set<String> likedContentIds = getUserLikedContent(userId);
            if (CollUtil.isEmpty(likedContentIds)) {
                // 用户未点赞任何内容，返回全0结果
                for (String contentType : contentTypes) {
                    result.put(contentType, 0L);
                }
                return result;
            }

            // 为每种内容类型初始化计数为0
            for (String contentType : contentTypes) {
                result.put(contentType, 0L);
            }

            // 对每个已点赞的内容，检查其类型并计数
            // 注意：这里假设内容ID可以解析出其类型，实际应用中可能需要从数据库查询
            // 这里仅作为示例实现，实际需要根据业务逻辑调整
            for (String contentId : likedContentIds) {
                // 此处仅为示例，实际应用中可能需要通过其他方式获取内容类型
                for (String contentType : contentTypes) {
                    if (contentId.startsWith(contentType + "_")) {
                        result.put(contentType, result.get(contentType) + 1);
                        break;
                    }
                }
            }

            return result;
        } catch (Exception e) {
            log.error("获取用户点赞类型分布异常: userId={}", userId, e);
            throw new LikeOperationException(LikeConstants.ErrorMessage.GET_LIKE_TYPE_DISTRIBUTION_FAILED);
        }
    }

    /**
     * 添加批量点赞功能（一次为多个内容点赞）
     *
     * @param userId      用户ID
     * @param contentType 内容类型
     * @param contentIds  内容ID列表
     * @return 成功点赞的内容ID列表
     */
    public List<String> batchAddLike(String userId, String contentType, List<String> contentIds) {
        if (StrUtil.isBlank(userId) || StrUtil.isBlank(contentType) || CollUtil.isEmpty(contentIds)) {
            throw new LikeOperationException(LikeConstants.ErrorMessage.EMPTY_PARAMS);
        }

        List<String> successList = Collections.synchronizedList(new ArrayList<>());
        
        // 对于较大的批次，使用并行流加快处理，小批次则使用顺序流避免额外开销
        Stream<String> stream = contentIds.size() > 100 ? contentIds.parallelStream() : contentIds.stream();
        stream.forEach(contentId -> {
            try {
                boolean success = addLike(userId, contentType, contentId);
                if (success) {
                    successList.add(contentId);
                }
            } catch (Exception e) {
                log.warn("单项点赞失败: userId={}, contentType={}, contentId={}", userId, contentType, contentId, e);
                // 继续处理下一个，不中断整个批量操作
            }
        });

        log.info("批量点赞完成: userId={}, contentType={}, 总数={}, 成功数={}", 
                userId, contentType, contentIds.size(), successList.size());
        return successList;
    }

    /**
     * 批量取消点赞（一次取消多个内容的点赞）
     *
     * @param userId      用户ID
     * @param contentType 内容类型
     * @param contentIds  内容ID列表
     * @return 成功取消点赞的内容ID列表
     */
    public List<String> batchRemoveLike(String userId, String contentType, List<String> contentIds) {
        if (StrUtil.isBlank(userId) || StrUtil.isBlank(contentType) || CollUtil.isEmpty(contentIds)) {
            throw new LikeOperationException(LikeConstants.ErrorMessage.EMPTY_PARAMS);
        }

        List<String> successList = Collections.synchronizedList(new ArrayList<>());
        
        // 对于较大的批次，使用并行流加快处理
        Stream<String> stream = contentIds.size() > 100 ? contentIds.parallelStream() : contentIds.stream();
        stream.forEach(contentId -> {
            try {
                boolean success = removeLike(userId, contentType, contentId);
                if (success) {
                    successList.add(contentId);
                }
            } catch (Exception e) {
                log.warn("单项取消点赞失败: userId={}, contentType={}, contentId={}", userId, contentType, contentId, e);
                // 继续处理下一个，不中断整个批量操作
            }
        });

        log.info("批量取消点赞完成: userId={}, contentType={}, 总数={}, 成功数={}", 
                userId, contentType, contentIds.size(), successList.size());
        return successList;
    }

    /**
     * 优化版本：根据热度值更新排行榜
     * 可用于手动调整内容热度
     *
     * @param contentType 内容类型
     * @param contentId   内容ID
     * @param score       热度分数
     */
    public void updateContentHotScore(String contentType, String contentId, double score) {
        if (StrUtil.hasBlank(contentType, contentId) || score < 0) {
            throw new LikeOperationException(LikeConstants.ErrorMessage.INVALID_PARAMS);
        }

        try {
            String rankKey = likeRedisKeyBuilder.buildRankKey(contentType);
            stringRedisTemplate.opsForZSet().add(rankKey, contentId, score);

            // 更新计数缓存，保持一致性
            String countKey = likeRedisKeyBuilder.buildCountKey(contentType, contentId);
            stringRedisTemplate.opsForValue().set(countKey, String.valueOf((long) score));

            // 设置过期时间
            stringRedisTemplate.expire(rankKey, defaultExpireDays, TimeUnit.DAYS);
            stringRedisTemplate.expire(countKey, defaultExpireDays, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("更新内容热度分数异常: contentType={}, contentId={}, score={}", contentType, contentId, score, e);
            throw new LikeOperationException(LikeConstants.ErrorMessage.UPDATE_HOT_SCORE_FAILED);
        }
    }

    /**
     * 重置内容点赞状态（清空所有点赞数据）
     * 注意：此操作会清除与指定内容相关的所有点赞数据，谨慎使用
     *
     * @param contentType 内容类型
     * @param contentId   内容ID
     * @return 是否成功
     */
    public boolean resetContentLikeStatus(String contentType, String contentId) {
        if (StrUtil.hasBlank(contentType, contentId)) {
            throw new LikeOperationException(LikeConstants.ErrorMessage.EMPTY_PARAMS);
        }

        try {
            // 检查是否分片
            boolean isSharded = isContentSharded(contentType, contentId);

            // 获取所有相关键
            String contentLikeKey = likeRedisKeyBuilder.buildContentLikeKey(contentType, contentId, null);
            String likeCountKey = likeRedisKeyBuilder.buildCountKey(contentType, contentId);
            String likeHllKey = likeRedisKeyBuilder.buildHllKey(contentType, contentId);
            String likeBfKey = likeRedisKeyBuilder.buildBfKey(contentType, contentId);
            String shardingKey = likeRedisKeyBuilder.buildShardingKey(contentType, contentId);
            String rankKey = likeRedisKeyBuilder.buildRankKey(contentType);

            // 如果已分片，删除所有分片键
            if (isSharded) {
                for (int i = 0; i < shardingBucketCount; i++) {
                    String shardKey = likeRedisKeyBuilder.buildContentLikeKey(contentType, contentId, i);
                    stringRedisTemplate.delete(shardKey);
                }
            }

            // 删除相关键
            stringRedisTemplate.delete(contentLikeKey);
            stringRedisTemplate.delete(likeCountKey);
            stringRedisTemplate.delete(likeHllKey);
            stringRedisTemplate.delete(likeBfKey);
            stringRedisTemplate.delete(shardingKey);

            // 从排行榜中移除
            stringRedisTemplate.opsForZSet().remove(rankKey, contentId);

            // 注意：此处没有从所有用户的点赞集合中移除该内容，因为可能涉及大量用户
            // 如需彻底清除，应考虑异步任务处理

            return true;
        } catch (Exception e) {
            log.error("重置内容点赞状态异常: contentType={}, contentId={}", contentType, contentId, e);
            throw new LikeOperationException(LikeConstants.ErrorMessage.RESET_LIKE_STATUS_FAILED);
        }
    }

    /**
     * 获取两个用户共同点赞的内容数量
     *
     * @param userId1 用户1 ID
     * @param userId2 用户2 ID
     * @return 共同点赞的内容数量
     */
    public long getCommonLikedContentCount(String userId1, String userId2) {
        if (StrUtil.hasBlank(userId1, userId2)) {
            throw new LikeOperationException(LikeConstants.ErrorMessage.EMPTY_PARAMS);
        }

        try {
            String userLikeKey1 = likeRedisKeyBuilder.buildUserLikeKey(userId1);
            String userLikeKey2 = likeRedisKeyBuilder.buildUserLikeKey(userId2);

            // 计算两个集合的交集大小
            Long count = stringRedisTemplate.opsForSet().intersectAndStore(userLikeKey1, userLikeKey2, "temp:intersect");

            // 删除临时交集键
            stringRedisTemplate.delete("temp:intersect");

            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("获取共同点赞内容数量异常: userId1={}, userId2={}", userId1, userId2, e);
            throw new LikeOperationException(LikeConstants.ErrorMessage.GET_COMMON_LIKES_FAILED);
        }
    }

    /**
     * 获取指定内容的点赞用户增长率
     * 通过记录不同时间点的点赞用户数，计算增长率
     *
     * @param contentType 内容类型
     * @param contentId   内容ID
     * @param timeUnit    时间单位（小时、天等）
     * @return 点赞用户增长率
     */
    public double getLikeGrowthRate(String contentType, String contentId, TimeUnit timeUnit) {
        // 实际实现需要与时间序列存储结合
        // 这里仅为示例，返回一个模拟值
        log.warn("获取点赞增长率功能需与时间序列存储结合，目前返回模拟值");

        // 模拟一个0-1之间的增长率
        return new Random().nextDouble();
    }
    
    /**
     * 验证基本参数是否有效
     */
    private void validateBasicParams(String userId, String contentType, String contentId) {
        if (StrUtil.hasBlank(userId, contentType, contentId)) {
            throw new LikeOperationException(LikeConstants.ErrorMessage.EMPTY_PARAMS);
        }
    }
} 