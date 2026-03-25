package com.ww.mall.promotion.engine;

import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.json.JacksonUtils;
import com.ww.app.redis.component.lua.RedisScriptComponent;
import com.ww.mall.promotion.constants.GroupBizConstants;
import com.ww.mall.promotion.engine.model.GroupCacheSnapshot;
import com.ww.mall.promotion.engine.model.GroupCommandResult;
import com.ww.mall.promotion.engine.model.GroupMemberCacheSnapshot;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.entity.group.GroupInstance;
import com.ww.mall.promotion.entity.group.GroupMember;
import com.ww.mall.promotion.enums.GroupMemberBizStatus;
import com.ww.mall.promotion.key.GroupRedisKeyBuilder;
import com.ww.mall.promotion.service.group.command.CreateGroupCommand;
import com.ww.mall.promotion.service.group.command.JoinGroupCommand;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_CREATE_FAILED;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_ERROR;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_NOT_EXISTS;

/**
 * 拼团存储组件。
 * <p>
 * 该组件统一收口拼团业务对 Redis 与 Mongo 的底层访问，包含：
 * Redis Lua 状态变更、Redis 快照读取、订单索引查询、成员辅助判断，以及 Mongo 投影同步与查询。
 * 业务服务仅负责命令编排、规则校验和异常语义，不再直接拼装底层存储细节。
 *
 * @author ww
 * @create 2026-03-25
 * @description: 统一封装拼团 Redis/Mongo 访问能力
 */
@Component
public class GroupStorageComponent {

    private static final String SCRIPT_GROUP_CREATE = "promotion_group_create";
    private static final String SCRIPT_GROUP_JOIN = "promotion_group_join";
    private static final String SCRIPT_GROUP_AFTER_SALE = "promotion_group_after_sale";
    private static final String SCRIPT_GROUP_EXPIRE = "promotion_group_expire";

    private static final String PATH_GROUP_CREATE = "lua/group/create_group.lua";
    private static final String PATH_GROUP_JOIN = "lua/group/join_group.lua";
    private static final String PATH_GROUP_AFTER_SALE = "lua/group/after_sale_success.lua";
    private static final String PATH_GROUP_EXPIRE = "lua/group/expire_mark_failed.lua";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private GroupRedisKeyBuilder groupRedisKeyBuilder;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private RedisScriptComponent redisScriptComponent;

    /**
     * 预加载拼团 Lua 脚本。
     * <p>
     * 统一通过 RedisScriptComponent 预加载脚本，运行时优先走 EVALSHA，
     * 避免每次执行都传输整段脚本文本，同时兼容 Redis 重启后的 NOSCRIPT 场景。
     */
    @PostConstruct
    public void init() {
        redisScriptComponent.preLoadLuaScript(SCRIPT_GROUP_CREATE, loadScriptBytes(PATH_GROUP_CREATE));
        redisScriptComponent.preLoadLuaScript(SCRIPT_GROUP_JOIN, loadScriptBytes(PATH_GROUP_JOIN));
        redisScriptComponent.preLoadLuaScript(SCRIPT_GROUP_AFTER_SALE, loadScriptBytes(PATH_GROUP_AFTER_SALE));
        redisScriptComponent.preLoadLuaScript(SCRIPT_GROUP_EXPIRE, loadScriptBytes(PATH_GROUP_EXPIRE));
    }

    /**
     * 执行开团 Redis Lua。
     *
     * @param groupId 团ID
     * @param activity 活动配置
     * @param command 开团命令
     * @param nowMillis 当前毫秒时间
     * @return 开团命令结果
     */
    public GroupCommandResult createGroup(String groupId, GroupActivity activity, CreateGroupCommand command, long nowMillis) {
        String activityUserCountField = groupRedisKeyBuilder.buildActivityUserCountField(activity.getId(), command.getUserId());
        List<String> keys = Arrays.asList(
                groupRedisKeyBuilder.buildGroupMetaKey(groupId),
                groupRedisKeyBuilder.buildGroupMemberStoreKey(groupId),
                groupRedisKeyBuilder.buildGroupUserIndexKey(groupId),
                groupRedisKeyBuilder.buildOrderIndexKey(),
                groupRedisKeyBuilder.buildActivityUserCountKey(),
                groupRedisKeyBuilder.buildExpiryIndexKey()
        );
        List<String> args = Arrays.asList(
                groupId,
                activity.getId(),
                String.valueOf(command.getUserId()),
                command.getOrderId(),
                String.valueOf(activity.getRequiredSize()),
                String.valueOf(activity.getSpuId()),
                buildMemberCachePayload(groupId, command.getUserId(), command.getOrderId(), true,
                        nowMillis, command.getPayAmount(), command.getSkuId(),
                        GroupMemberBizStatus.JOINED.name(), null, "PAY_SUCCESS", nowMillis),
                String.valueOf(nowMillis),
                String.valueOf(buildExpireTime(nowMillis, activity)),
                String.valueOf(defaultLimitPerUser(activity.getLimitPerUser())),
                activityUserCountField,
                String.valueOf(GroupBizConstants.REDIS_GROUP_DATA_RETAIN_SECONDS)
        );
        return parseCreateResult(executeMultiScript(SCRIPT_GROUP_CREATE, keys, args));
    }

    /**
     * 执行参团 Redis Lua。
     *
     * @param activity 活动配置
     * @param command 参团命令
     * @param nowMillis 当前毫秒时间
     * @return 参团命令结果
     */
    public GroupCommandResult joinGroup(GroupActivity activity, JoinGroupCommand command, long nowMillis) {
        String activityUserCountField = groupRedisKeyBuilder.buildActivityUserCountField(activity.getId(), command.getUserId());
        List<String> keys = Arrays.asList(
                groupRedisKeyBuilder.buildGroupMetaKey(command.getGroupId()),
                groupRedisKeyBuilder.buildGroupMemberStoreKey(command.getGroupId()),
                groupRedisKeyBuilder.buildGroupUserIndexKey(command.getGroupId()),
                groupRedisKeyBuilder.buildOrderIndexKey(),
                groupRedisKeyBuilder.buildActivityUserCountKey(),
                groupRedisKeyBuilder.buildExpiryIndexKey()
        );
        List<String> args = Arrays.asList(
                command.getGroupId(),
                activity.getId(),
                String.valueOf(command.getUserId()),
                command.getOrderId(),
                String.valueOf(defaultLimitPerUser(activity.getLimitPerUser())),
                buildMemberCachePayload(command.getGroupId(), command.getUserId(), command.getOrderId(), false,
                        nowMillis, command.getPayAmount(), command.getSkuId(),
                        GroupMemberBizStatus.JOINED.name(), null, "PAY_SUCCESS", nowMillis),
                activityUserCountField,
                String.valueOf(nowMillis),
                String.valueOf(GroupBizConstants.REDIS_GROUP_DATA_RETAIN_SECONDS)
        );
        return parseJoinResult(executeMultiScript(SCRIPT_GROUP_JOIN, keys, args));
    }

    /**
     * 执行售后成功 Redis Lua。
     *
     * @param groupId 团ID
     * @param activityId 活动ID
     * @param afterSaleId 售后单号
     * @param orderId 订单号
     * @param nowMillis 事件时间
     * @param failReason 关团原因
     * @param reason 售后原因
     * @return Lua 返回码
     */
    public int afterSaleSuccess(String groupId, String activityId, String afterSaleId,
                                String orderId, long nowMillis, String failReason, String reason) {
        List<String> keys = Arrays.asList(
                groupRedisKeyBuilder.buildGroupMetaKey(groupId),
                groupRedisKeyBuilder.buildGroupMemberStoreKey(groupId),
                groupRedisKeyBuilder.buildGroupUserIndexKey(groupId),
                groupRedisKeyBuilder.buildActivityUserCountKey(),
                groupRedisKeyBuilder.buildExpiryIndexKey()
        );
        List<String> args = Arrays.asList(
                groupId,
                activityId,
                nullSafe(afterSaleId),
                orderId,
                String.valueOf(nowMillis),
                failReason,
                nullSafe(reason),
                String.valueOf(GroupBizConstants.REDIS_GROUP_DATA_RETAIN_SECONDS),
                groupRedisKeyBuilder.buildActivityUserCountFieldPrefix(activityId)
        );
        return parseLuaCode(executeMultiScript(SCRIPT_GROUP_AFTER_SALE, keys, args));
    }

    /**
     * 执行过期关团 Redis Lua。
     *
     * @param groupId 团ID
     * @param activityId 活动ID
     * @param reason 关团原因
     * @param nowMillis 当前毫秒时间
     * @return Lua 返回码
     */
    public int expireGroup(String groupId, String activityId, String reason, long nowMillis) {
        List<String> keys = Arrays.asList(
                groupRedisKeyBuilder.buildGroupMetaKey(groupId),
                groupRedisKeyBuilder.buildGroupMemberStoreKey(groupId),
                groupRedisKeyBuilder.buildGroupUserIndexKey(groupId),
                groupRedisKeyBuilder.buildActivityUserCountKey(),
                groupRedisKeyBuilder.buildExpiryIndexKey()
        );
        List<String> args = Arrays.asList(
                groupId,
                reason,
                String.valueOf(nowMillis),
                String.valueOf(GroupBizConstants.REDIS_GROUP_DATA_RETAIN_SECONDS),
                groupRedisKeyBuilder.buildActivityUserCountFieldPrefix(activityId)
        );
        return parseLuaCode(executeMultiScript(SCRIPT_GROUP_EXPIRE, keys, args));
    }

    /**
     * 根据订单索引查询拼团ID。
     *
     * @param orderId 订单ID
     * @return 拼团ID，不存在时返回 {@code null}
     */
    public String findGroupIdByOrderId(String orderId) {
        return (String) stringRedisTemplate.opsForHash().get(groupRedisKeyBuilder.buildOrderIndexKey(), orderId);
    }

    /**
     * 查询已到期的拼团ID列表。
     * <p>
     * 该方法仅负责按过期索引读取当前时间之前到期的团ID，不直接在此处修改状态。
     * 实际关团仍由上层命令服务统一调用 Redis Lua 完成，保证多实例下的原子性与幂等性。
     *
     * @param nowMillis 当前毫秒时间
     * @param limit 批量上限
     * @return 已到期的团ID集合，不存在时返回空集合
     */
    public Set<String> findExpiredGroupIds(long nowMillis, long limit) {
        Set<String> expiredGroupIds = stringRedisTemplate.opsForZSet().rangeByScore(
                groupRedisKeyBuilder.buildExpiryIndexKey(),
                0,
                nowMillis,
                0,
                limit
        );
        return expiredGroupIds == null ? Collections.emptySet() : expiredGroupIds;
    }

    /**
     * 读取 Redis 拼团快照。
     *
     * @param groupId 团ID
     * @return 拼团快照，不存在时返回 {@code null}
     */
    public GroupCacheSnapshot loadGroupSnapshot(String groupId) {
        if (!hasText(groupId)) {
            return null;
        }
        Map<Object, Object> metaMap = stringRedisTemplate.opsForHash().entries(groupRedisKeyBuilder.buildGroupMetaKey(groupId));
        if (metaMap.isEmpty()) {
            return null;
        }
        GroupCacheSnapshot snapshot = new GroupCacheSnapshot();
        List<GroupMember> members = loadMembers(groupId);
        GroupInstance instance = buildInstance(groupId, metaMap, members);
        snapshot.setInstance(instance);
        snapshot.setMembers(members);
        return snapshot;
    }

    /**
     * 读取必定存在的 Redis 拼团快照。
     *
     * @param groupId 团ID
     * @return 拼团快照
     */
    public GroupCacheSnapshot requireGroupSnapshot(String groupId) {
        GroupCacheSnapshot snapshot = loadGroupSnapshot(groupId);
        if (snapshot == null || snapshot.getInstance() == null) {
            throw new ApiException(GROUP_RECORD_NOT_EXISTS);
        }
        return snapshot;
    }

    /**
     * 将 Redis 快照同步到 Mongo 投影。
     *
     * @param groupId 团ID
     * @return 最新快照
     */
    public GroupCacheSnapshot syncProjection(String groupId) {
        GroupCacheSnapshot snapshot = requireGroupSnapshot(groupId);
        syncProjection(snapshot);
        return snapshot;
    }

    /**
     * 将指定快照同步到 Mongo 投影。
     *
     * @param snapshot 拼团快照
     */
    public void syncProjection(GroupCacheSnapshot snapshot) {
        if (snapshot == null || snapshot.getInstance() == null) {
            return;
        }
        mongoTemplate.save(snapshot.getInstance());
        upsertMembers(snapshot.getMembers());
    }

    /**
     * 从快照中按订单号查找成员用户ID。
     *
     * @param snapshot 拼团快照
     * @param orderId 订单ID
     * @return 用户ID，不存在时返回 {@code null}
     */
    public Long findMemberUserId(GroupCacheSnapshot snapshot, String orderId) {
        if (snapshot == null || snapshot.getMembers() == null) {
            return null;
        }
        return snapshot.getMembers().stream()
                .filter(member -> orderId.equals(member.getOrderId()))
                .map(GroupMember::getUserId)
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断指定用户是否为团长。
     *
     * @param snapshot 拼团快照
     * @param userId 用户ID
     * @return true-团长，false-非团长
     */
    public boolean isLeader(GroupCacheSnapshot snapshot, Long userId) {
        return snapshot != null && snapshot.getInstance() != null
                && userId != null && userId.equals(snapshot.getInstance().getLeaderUserId());
    }

    /**
     * 读取 Mongo 拼团实例详情。
     *
     * @param groupId 团ID
     * @return Mongo 拼团实例
     */
    public GroupInstance findMongoGroupInstance(String groupId) {
        return mongoTemplate.findOne(GroupInstance.buildIdQuery(groupId), GroupInstance.class);
    }

    /**
     * 读取 Mongo 拼团成员详情。
     *
     * @param groupId 团ID
     * @return 成员列表
     */
    public List<GroupMember> findMongoGroupMembers(String groupId) {
        return mongoTemplate.find(GroupMember.buildGroupInstanceIdQuery(groupId), GroupMember.class);
    }

    /**
     * 查询用户参与的 Mongo 拼团成员记录。
     *
     * @param userId 用户ID
     * @return 成员记录列表
     */
    public List<GroupMember> findMongoUserMembers(Long userId) {
        return mongoTemplate.find(GroupMember.buildUserIdQuery(userId), GroupMember.class);
    }

    /**
     * 查询活动下的 Mongo 拼团摘要。
     *
     * @param activityId 活动ID
     * @param status 团状态
     * @return 团摘要列表
     */
    public List<GroupInstance> findMongoActivityGroupSummaries(String activityId, String status) {
        return mongoTemplate.find(GroupInstance.buildActivityIdAndStatusSummaryQuery(activityId, status), GroupInstance.class);
    }

    /**
     * 批量查询 Mongo 拼团摘要。
     *
     * @param groupIds 团ID列表
     * @return 团摘要列表
     */
    public List<GroupInstance> findMongoGroupSummaries(List<String> groupIds) {
        return mongoTemplate.find(GroupInstance.buildIdListSummaryQuery(groupIds), GroupInstance.class);
    }

    /**
     * 执行返回多值结果的 Lua 脚本。
     *
     * @param scriptName 脚本名称
     * @param keys Redis keys
     * @param args Lua 参数
     * @return Lua 原始返回值
     */
    private List<Object> executeMultiScript(String scriptName, List<String> keys, List<String> args) {
        return redisScriptComponent.executeLuaScript(scriptName, ReturnType.MULTI, keys, args);
    }

    /**
     * 按订单号增量 upsert 成员轨迹。
     *
     * @param members 成员快照
     */
    private void upsertMembers(List<GroupMember> members) {
        if (members == null || members.isEmpty()) {
            return;
        }
        BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, GroupMember.class);
        Date now = new Date();
        int upsertCount = 0;
        for (GroupMember member : members) {
            if (member == null || !hasText(member.getOrderId())) {
                continue;
            }
            bulkOperations.upsert(
                    GroupMember.buildOrderIdQuery(member.getOrderId()),
                    GroupMember.buildOrderIdUpsert(member, now)
            );
            upsertCount++;
        }
        if (upsertCount > 0) {
            bulkOperations.execute();
        }
    }

    /**
     * 读取成员快照。
     *
     * @param groupId 团ID
     * @return 成员列表
     */
    private List<GroupMember> loadMembers(String groupId) {
        List<Object> memberJsonList = stringRedisTemplate.opsForHash()
                .values(groupRedisKeyBuilder.buildGroupMemberStoreKey(groupId));
        if (memberJsonList.isEmpty()) {
            return new ArrayList<>();
        }
        List<GroupMember> members = new ArrayList<>();
        for (Object memberJson : memberJsonList) {
            if (memberJson == null) {
                continue;
            }
            members.add(buildMember(String.valueOf(memberJson)));
        }
        members.sort((left, right) -> compareDate(left.getJoinTime(), right.getJoinTime()));
        return members;
    }

    /**
     * 构建团实例。
     *
     * @param groupId 团ID
     * @param metaMap 主状态Hash
     * @param members 成员列表
     * @return 团实例
     */
    private GroupInstance buildInstance(String groupId, Map<Object, Object> metaMap, List<GroupMember> members) {
        GroupInstance instance = new GroupInstance();
        instance.setId(groupId);
        instance.setActivityId(getString(metaMap, "activityId"));
        instance.setLeaderUserId(getLong(metaMap, "leaderUserId"));
        instance.setStatus(getString(metaMap, "status"));
        instance.setRequiredSize(getInteger(metaMap, "requiredSize"));
        instance.setCurrentSize(getInteger(metaMap, "currentSize"));
        instance.setRemainingSlots(getInteger(metaMap, "remainingSlots"));
        instance.setExpireTime(getDate(metaMap, "expireTime"));
        instance.setCompleteTime(getDate(metaMap, "completeTime"));
        instance.setFailedTime(getDate(metaMap, "failedTime"));
        instance.setSpuId(getLong(metaMap, "spuId"));
        instance.setFailReason(getString(metaMap, "failReason"));
        instance.setCreateTime(getDate(metaMap, "createTime"));
        instance.setUpdateTime(getDate(metaMap, "updateTime"));
        instance.setSkuIds(members.stream()
                .map(GroupMember::getSkuId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()));
        instance.setMembers(members.stream()
                .map(this::buildMemberSummary)
                .collect(Collectors.toList()));
        return instance;
    }

    /**
     * 构建成员对象。
     *
     * @param memberJson 成员JSON
     * @return 成员对象
     */
    private GroupMember buildMember(String memberJson) {
        try {
            GroupMemberCacheSnapshot cacheSnapshot = JacksonUtils.parseObject(memberJson, GroupMemberCacheSnapshot.class);
            GroupMember member = new GroupMember();
            member.setGroupInstanceId(cacheSnapshot.getGroupInstanceId());
            member.setUserId(cacheSnapshot.getUserId());
            member.setOrderId(cacheSnapshot.getOrderId());
            member.setIsLeader(cacheSnapshot.getIsLeader());
            member.setJoinTime(toDate(cacheSnapshot.getJoinTime()));
            member.setPayAmount(cacheSnapshot.getPayAmount());
            member.setSkuId(cacheSnapshot.getSkuId());
            member.setMemberStatus(cacheSnapshot.getMemberStatus());
            member.setAfterSaleId(cacheSnapshot.getAfterSaleId());
            member.setLatestTrajectory(cacheSnapshot.getLatestTrajectory());
            member.setLatestTrajectoryTime(toDate(cacheSnapshot.getLatestTrajectoryTime()));
            member.setCreateTime(member.getJoinTime());
            member.setUpdateTime(member.getLatestTrajectoryTime() != null ? member.getLatestTrajectoryTime() : member.getJoinTime());
            return member;
        } catch (Exception e) {
            throw new IllegalStateException("拼团成员缓存反序列化失败", e);
        }
    }

    /**
     * 构建成员摘要。
     *
     * @param member 成员
     * @return 成员摘要
     */
    private GroupInstance.GroupMemberInfo buildMemberSummary(GroupMember member) {
        GroupInstance.GroupMemberInfo memberInfo = new GroupInstance.GroupMemberInfo();
        memberInfo.setUserId(member.getUserId());
        memberInfo.setOrderId(member.getOrderId());
        memberInfo.setSkuId(member.getSkuId());
        memberInfo.setJoinTime(member.getJoinTime());
        memberInfo.setIsLeader(member.getIsLeader() != null && member.getIsLeader() == 1);
        memberInfo.setMemberStatus(member.getMemberStatus());
        memberInfo.setLatestTrajectory(member.getLatestTrajectory());
        memberInfo.setLatestTrajectoryTime(member.getLatestTrajectoryTime());
        return memberInfo;
    }

    /**
     * 解析开团 Lua 返回值。
     *
     * @param rawResult Lua 原始返回
     * @return 命令结果
     */
    private GroupCommandResult parseCreateResult(List<Object> rawResult) {
        GroupCommandResult result = new GroupCommandResult();
        if (rawResult == null || rawResult.isEmpty()) {
            result.setFailReason(GROUP_CREATE_FAILED.getMsg());
            return result;
        }
        int code = parseInt(rawResult.get(0));
        if (code == 1) {
            result.setSuccess(true);
            result.setGroupId(stringValue(rawResult, 1));
            return result;
        }
        if (code == 2) {
            result.setSuccess(true);
            result.setGroupId(stringValue(rawResult, 1));
            result.setReplayed(true);
            return result;
        }
        result.setFailReason(String.valueOf(code));
        return result;
    }

    /**
     * 解析参团 Lua 返回值。
     *
     * @param rawResult Lua 原始返回
     * @return 命令结果
     */
    private GroupCommandResult parseJoinResult(List<Object> rawResult) {
        GroupCommandResult result = new GroupCommandResult();
        if (rawResult == null || rawResult.isEmpty()) {
            result.setFailReason(GROUP_RECORD_ERROR.getMsg());
            return result;
        }
        int code = parseInt(rawResult.get(0));
        if (code == 1) {
            result.setSuccess(true);
            result.setGroupId(stringValue(rawResult, 1));
            result.setGroupStatus(stringValue(rawResult, 2));
            return result;
        }
        if (code == 2) {
            result.setSuccess(true);
            result.setGroupId(stringValue(rawResult, 1));
            result.setReplayed(true);
            return result;
        }
        result.setFailReason(String.valueOf(code));
        if (code == -4) {
            result.setGroupStatus(stringValue(rawResult, 1));
        } else {
            result.setGroupId(stringValue(rawResult, 1));
        }
        return result;
    }

    /**
     * 解析仅返回状态码的 Lua 结果。
     *
     * @param rawResult Lua 原始返回
     * @return 状态码
     */
    private int parseLuaCode(List<Object> rawResult) {
        if (rawResult == null || rawResult.isEmpty()) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        return parseInt(rawResult.get(0));
    }

    /**
     * 构建成员缓存 JSON。
     *
     * @param groupId 团ID
     * @param userId 用户ID
     * @param orderId 订单ID
     * @param leader 是否团长
     * @param joinTime 入团时间
     * @param payAmount 支付金额
     * @param skuId SKU ID
     * @param memberStatus 成员状态
     * @param afterSaleId 售后单号
     * @param latestTrajectory 最新轨迹编码
     * @param latestTrajectoryTime 最新轨迹时间
     * @return JSON 文本
     */
    private String buildMemberCachePayload(String groupId, Long userId, String orderId, boolean leader,
                                           long joinTime, BigDecimal payAmount, Long skuId, String memberStatus,
                                           String afterSaleId, String latestTrajectory, long latestTrajectoryTime) {
        GroupMemberCacheSnapshot member = new GroupMemberCacheSnapshot();
        member.setGroupInstanceId(groupId);
        member.setUserId(userId);
        member.setOrderId(orderId);
        member.setIsLeader(leader ? 1 : 0);
        member.setJoinTime(joinTime);
        member.setPayAmount(payAmount);
        member.setSkuId(skuId);
        member.setMemberStatus(memberStatus);
        member.setAfterSaleId(afterSaleId);
        member.setLatestTrajectory(latestTrajectory);
        member.setLatestTrajectoryTime(latestTrajectoryTime);
        try {
            return JacksonUtils.toJsonString(member);
        } catch (Exception e) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
    }

    /**
     * 计算团过期时间。
     *
     * @param nowMillis 当前毫秒时间
     * @param activity 活动配置
     * @return 过期毫秒时间
     */
    private long buildExpireTime(long nowMillis, GroupActivity activity) {
        return nowMillis + activity.getExpireHours() * 3600_000L;
    }

    /**
     * 获取默认限购值。
     *
     * @param limitPerUser 限购值
     * @return 非空限购值
     */
    private int defaultLimitPerUser(Integer limitPerUser) {
        return limitPerUser == null ? 0 : limitPerUser;
    }

    /**
     * 比较时间。
     *
     * @param left 左时间
     * @param right 右时间
     * @return 比较结果
     */
    private int compareDate(Date left, Date right) {
        Date leftValue = left != null ? left : new Date(0L);
        Date rightValue = right != null ? right : new Date(0L);
        return leftValue.compareTo(rightValue);
    }

    /**
     * 毫秒值转日期。
     *
     * @param millis 毫秒值
     * @return 日期
     */
    private Date toDate(Long millis) {
        return millis == null || millis <= 0 ? null : new Date(millis);
    }

    /**
     * 读取字符串。
     *
     * @param map 数据Map
     * @param key 字段名
     * @return 字符串值
     */
    private String getString(Map<Object, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 读取长整型。
     *
     * @param map 数据Map
     * @param key 字段名
     * @return 长整型值
     */
    private Long getLong(Map<Object, Object> map, String key) {
        String value = getString(map, key);
        return hasText(value) ? Long.parseLong(value) : null;
    }

    /**
     * 读取整型。
     *
     * @param map 数据Map
     * @param key 字段名
     * @return 整型值
     */
    private Integer getInteger(Map<Object, Object> map, String key) {
        String value = getString(map, key);
        return hasText(value) ? Integer.parseInt(value) : null;
    }

    /**
     * 读取日期。
     *
     * @param map 数据Map
     * @param key 字段名
     * @return 日期
     */
    private Date getDate(Map<Object, Object> map, String key) {
        Long millis = getLong(map, key);
        return millis == null || millis <= 0 ? null : new Date(millis);
    }

    /**
     * 解析整型值。
     *
     * @param value 原值
     * @return 整型结果
     */
    private int parseInt(Object value) {
        return Integer.parseInt(String.valueOf(value));
    }

    /**
     * 读取列表中的字符串项。
     *
     * @param values 返回列表
     * @param index 下标
     * @return 字符串值
     */
    private String stringValue(List<Object> values, int index) {
        return values.size() > index && values.get(index) != null ? String.valueOf(values.get(index)) : null;
    }

    /**
     * 空值保护。
     *
     * @param value 原值
     * @return 非 null 字符串
     */
    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    /**
     * 从 classpath 读取 Lua 脚本字节。
     *
     * @param path 资源路径
     * @return 脚本字节
     */
    private byte[] loadScriptBytes(String path) {
        try {
            return StreamUtils.copyToByteArray(new ClassPathResource(path).getInputStream());
        } catch (IOException e) {
            throw new IllegalStateException("加载拼团 Lua 脚本失败: " + path, e);
        }
    }

    /**
     * 判断文本是否有值。
     *
     * @param value 文本
     * @return true-有值
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
