package com.ww.mall.promotion.component;

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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
public class GroupStorageComponent {

    private static final String FIELD_OPEN_GROUP_COUNT = "openGroupCount";
    private static final String FIELD_JOIN_MEMBER_COUNT = "joinMemberCount";

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
        log.info("预加载拼团 Lua 脚本开始");
        redisScriptComponent.preLoadLuaScript(SCRIPT_GROUP_CREATE, loadScriptBytes(PATH_GROUP_CREATE));
        redisScriptComponent.preLoadLuaScript(SCRIPT_GROUP_JOIN, loadScriptBytes(PATH_GROUP_JOIN));
        redisScriptComponent.preLoadLuaScript(SCRIPT_GROUP_AFTER_SALE, loadScriptBytes(PATH_GROUP_AFTER_SALE));
        redisScriptComponent.preLoadLuaScript(SCRIPT_GROUP_EXPIRE, loadScriptBytes(PATH_GROUP_EXPIRE));
        log.info("预加载拼团 Lua 脚本完成: scripts=[{},{},{},{}]",
                SCRIPT_GROUP_CREATE, SCRIPT_GROUP_JOIN, SCRIPT_GROUP_AFTER_SALE, SCRIPT_GROUP_EXPIRE);
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
        log.debug("执行开团存储操作: groupId={}, activityId={}, userId={}, orderId={}, nowMillis={}",
                groupId, activity.getId(), command.getUserId(), command.getOrderId(), nowMillis);
        String activityUserCountField = groupRedisKeyBuilder.buildActivityUserCountField(activity.getId(), command.getUserId());
        List<String> keys = Arrays.asList(
                groupRedisKeyBuilder.buildGroupMetaKey(groupId),
                groupRedisKeyBuilder.buildGroupMemberStoreKey(groupId),
                groupRedisKeyBuilder.buildGroupUserIndexKey(groupId),
                groupRedisKeyBuilder.buildOrderIndexKey(),
                groupRedisKeyBuilder.buildActivityUserCountKey(),
                groupRedisKeyBuilder.buildActivityStatsKey(activity.getId()),
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
        GroupCommandResult result = parseCreateResult(executeMultiScript(SCRIPT_GROUP_CREATE, keys, args));
        log.debug("开团存储操作完成: groupId={}, success={}, replayed={}, failReason={}",
                result.getGroupId(), result.isSuccess(), result.isReplayed(), result.getFailReason());
        return result;
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
        log.debug("执行参团存储操作: groupId={}, activityId={}, userId={}, orderId={}, nowMillis={}",
                command.getGroupId(), activity.getId(), command.getUserId(), command.getOrderId(), nowMillis);
        String activityUserCountField = groupRedisKeyBuilder.buildActivityUserCountField(activity.getId(), command.getUserId());
        List<String> keys = Arrays.asList(
                groupRedisKeyBuilder.buildGroupMetaKey(command.getGroupId()),
                groupRedisKeyBuilder.buildGroupMemberStoreKey(command.getGroupId()),
                groupRedisKeyBuilder.buildGroupUserIndexKey(command.getGroupId()),
                groupRedisKeyBuilder.buildOrderIndexKey(),
                groupRedisKeyBuilder.buildActivityUserCountKey(),
                groupRedisKeyBuilder.buildActivityStatsKey(activity.getId()),
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
        GroupCommandResult result = parseJoinResult(executeMultiScript(SCRIPT_GROUP_JOIN, keys, args));
        log.debug("参团存储操作完成: groupId={}, success={}, replayed={}, groupStatus={}, failReason={}",
                result.getGroupId(), result.isSuccess(), result.isReplayed(), result.getGroupStatus(), result.getFailReason());
        return result;
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
        log.debug("执行售后成功存储操作: groupId={}, activityId={}, afterSaleId={}, orderId={}, nowMillis={}",
                groupId, activityId, afterSaleId, orderId, nowMillis);
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
        int code = parseLuaCode(executeMultiScript(SCRIPT_GROUP_AFTER_SALE, keys, args));
        log.debug("售后成功存储操作完成: groupId={}, orderId={}, code={}", groupId, orderId, code);
        return code;
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
        log.debug("执行过期关团存储操作: groupId={}, activityId={}, reason={}, nowMillis={}",
                groupId, activityId, reason, nowMillis);
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
        int code = parseLuaCode(executeMultiScript(SCRIPT_GROUP_EXPIRE, keys, args));
        log.debug("过期关团存储操作完成: groupId={}, code={}", groupId, code);
        return code;
    }

    /**
     * 根据订单索引查询拼团ID。
     *
     * @param orderId 订单ID
     * @return 拼团ID，不存在时返回 {@code null}
     */
    public String findGroupIdByOrderId(String orderId) {
        String groupId = (String) stringRedisTemplate.opsForHash().get(groupRedisKeyBuilder.buildOrderIndexKey(), orderId);
        log.debug("根据订单索引查询拼团ID: orderId={}, groupId={}", orderId, groupId);
        return groupId;
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
        log.debug("查询已到期拼团ID: nowMillis={}, limit={}", nowMillis, limit);
        Set<String> expiredGroupIds = stringRedisTemplate.opsForZSet().rangeByScore(
                groupRedisKeyBuilder.buildExpiryIndexKey(),
                0,
                nowMillis,
                0,
                limit
        );
        Set<String> result = expiredGroupIds == null ? Collections.emptySet() : expiredGroupIds;
        log.debug("查询已到期拼团ID完成: nowMillis={}, limit={}, count={}", nowMillis, limit, result.size());
        return result;
    }

    /**
     * 读取 Redis 拼团快照。
     *
     * @param groupId 团ID
     * @return 拼团快照，不存在时返回 {@code null}
     */
    public GroupCacheSnapshot loadGroupSnapshot(String groupId) {
        log.debug("读取拼团Redis快照: groupId={}", groupId);
        if (!hasText(groupId)) {
            log.warn("读取拼团Redis快照失败: groupId为空");
            return null;
        }
        Map<Object, Object> metaMap = stringRedisTemplate.opsForHash().entries(groupRedisKeyBuilder.buildGroupMetaKey(groupId));
        if (metaMap.isEmpty()) {
            log.debug("拼团Redis快照不存在: groupId={}", groupId);
            return null;
        }
        GroupCacheSnapshot snapshot = new GroupCacheSnapshot();
        List<GroupMember> members = loadMembers(groupId);
        GroupInstance instance = buildInstance(groupId, metaMap, members);
        snapshot.setInstance(instance);
        snapshot.setMembers(members);
        log.debug("读取拼团Redis快照完成: groupId={}, status={}, memberCount={}",
                groupId, instance.getStatus(), members.size());
        return snapshot;
    }

    /**
     * 读取必定存在的 Redis 拼团快照。
     *
     * @param groupId 团ID
     * @return 拼团快照
     */
    public GroupCacheSnapshot requireGroupSnapshot(String groupId) {
        log.debug("读取必需存在的拼团Redis快照: groupId={}", groupId);
        GroupCacheSnapshot snapshot = loadGroupSnapshot(groupId);
        if (snapshot == null || snapshot.getInstance() == null) {
            log.warn("必需存在的拼团Redis快照缺失: groupId={}", groupId);
            throw new ApiException(GROUP_RECORD_NOT_EXISTS);
        }
        log.debug("读取必需存在的拼团Redis快照完成: groupId={}, status={}",
                groupId, snapshot.getInstance().getStatus());
        return snapshot;
    }

    /**
     * 将 Redis 快照同步到 Mongo 投影。
     *
     * @param groupId 团ID
     * @return 最新快照
     */
    public GroupCacheSnapshot syncProjection(String groupId) {
        log.debug("按groupId同步Mongo投影: groupId={}", groupId);
        GroupCacheSnapshot snapshot = requireGroupSnapshot(groupId);
        syncProjection(snapshot);
        log.debug("按groupId同步Mongo投影完成: groupId={}, memberCount={}",
                groupId, snapshot.getMembers() == null ? 0 : snapshot.getMembers().size());
        return snapshot;
    }

    /**
     * 将指定快照同步到 Mongo 投影。
     *
     * @param snapshot 拼团快照
     */
    public void syncProjection(GroupCacheSnapshot snapshot) {
        if (snapshot == null || snapshot.getInstance() == null) {
            log.warn("同步Mongo投影跳过: snapshot为空或instance为空");
            return;
        }
        log.debug("同步Mongo投影: groupId={}, status={}, memberCount={}",
                snapshot.getInstance().getId(),
                snapshot.getInstance().getStatus(),
                snapshot.getMembers() == null ? 0 : snapshot.getMembers().size());
        mongoTemplate.save(snapshot.getInstance());
        upsertMembers(snapshot.getMembers());
        log.debug("同步Mongo投影完成: groupId={}", snapshot.getInstance().getId());
    }

    /**
     * 从快照中按订单号查找成员用户ID。
     *
     * @param snapshot 拼团快照
     * @param orderId 订单ID
     * @return 用户ID，不存在时返回 {@code null}
     */
    public Long findMemberUserId(GroupCacheSnapshot snapshot, String orderId) {
        log.debug("根据订单号查询成员用户ID: orderId={}", orderId);
        if (snapshot == null || snapshot.getMembers() == null) {
            log.warn("根据订单号查询成员用户ID失败: snapshot为空或members为空, orderId={}", orderId);
            return null;
        }
        Long userId = snapshot.getMembers().stream()
                .filter(member -> orderId.equals(member.getOrderId()))
                .map(GroupMember::getUserId)
                .findFirst()
                .orElse(null);
        log.debug("根据订单号查询成员用户ID完成: orderId={}, userId={}", orderId, userId);
        return userId;
    }

    /**
     * 判断指定用户是否为团长。
     *
     * @param snapshot 拼团快照
     * @param userId 用户ID
     * @return true-团长，false-非团长
     */
    public boolean isLeader(GroupCacheSnapshot snapshot, Long userId) {
        boolean leader = snapshot != null && snapshot.getInstance() != null
                && userId != null && userId.equals(snapshot.getInstance().getLeaderUserId());
        log.debug("判断用户是否团长: groupId={}, userId={}, leader={}",
                snapshot != null && snapshot.getInstance() != null ? snapshot.getInstance().getId() : null,
                userId, leader);
        return leader;
    }

    /**
     * 读取 Mongo 拼团实例详情。
     *
     * @param groupId 团ID
     * @return Mongo 拼团实例
     */
    public GroupInstance findMongoGroupInstance(String groupId) {
        GroupInstance instance = mongoTemplate.findOne(GroupInstance.buildIdQuery(groupId), GroupInstance.class);
        log.debug("查询Mongo拼团实例: groupId={}, found={}", groupId, instance != null);
        return instance;
    }

    /**
     * 读取 Mongo 拼团成员详情。
     *
     * @param groupId 团ID
     * @return 成员列表
     */
    public List<GroupMember> findMongoGroupMembers(String groupId) {
        List<GroupMember> members = mongoTemplate.find(GroupMember.buildGroupInstanceIdQuery(groupId), GroupMember.class);
        log.debug("查询Mongo拼团成员: groupId={}, memberCount={}", groupId, members.size());
        return members;
    }

    /**
     * 查询用户参与的 Mongo 拼团成员记录。
     *
     * @param userId 用户ID
     * @return 成员记录列表
     */
    public List<GroupMember> findMongoUserMembers(Long userId) {
        List<GroupMember> members = mongoTemplate.find(GroupMember.buildUserIdQuery(userId), GroupMember.class);
        log.debug("查询用户Mongo拼团成员记录: userId={}, memberCount={}", userId, members.size());
        return members;
    }

    /**
     * 查询活动下的 Mongo 拼团摘要。
     *
     * @param activityId 活动ID
     * @param status 团状态
     * @return 团摘要列表
     */
    public List<GroupInstance> findMongoActivityGroupSummaries(String activityId, String status) {
        List<GroupInstance> instances = mongoTemplate.find(
                GroupInstance.buildActivityIdAndStatusSummaryQuery(activityId, status),
                GroupInstance.class
        );
        log.debug("查询活动Mongo拼团摘要: activityId={}, status={}, count={}", activityId, status, instances.size());
        return instances;
    }

    /**
     * 批量查询 Mongo 拼团摘要。
     *
     * @param groupIds 团ID列表
     * @return 团摘要列表
     */
    public List<GroupInstance> findMongoGroupSummaries(List<String> groupIds) {
        List<GroupInstance> instances = mongoTemplate.find(GroupInstance.buildIdListSummaryQuery(groupIds), GroupInstance.class);
        log.debug("批量查询Mongo拼团摘要: groupIdCount={}, resultCount={}",
                groupIds == null ? 0 : groupIds.size(), instances.size());
        return instances;
    }

    /**
     * 执行返回多值结果的 Lua 脚本。
     *
     * @param scriptName 脚本名称
     * @param keys Redis keys
     * @param args Lua 参数
     * @return Lua 原始返回值
     */
    /**
     * 回填单个活动的统计数据。
     * <p>
     * 当前仅回填 Redis 中维护的累计开团数与累计参团人数。
     * 若活动尚无统计记录，则按 0 回填，避免调用方额外处理空值。
     *
     * @param activity 活动实体
     */
    public void fillActivityStatistics(GroupActivity activity) {
        if (activity == null || !hasText(activity.getId())) {
            log.warn("回填活动统计跳过: activity为空或activityId为空");
            return;
        }
        log.debug("回填单个活动统计: activityId={}", activity.getId());
        Map<Object, Object> statsMap = stringRedisTemplate.opsForHash()
                .entries(groupRedisKeyBuilder.buildActivityStatsKey(activity.getId()));
        applyActivityStatistics(activity, statsMap);
        log.debug("回填单个活动统计完成: activityId={}, openGroupCount={}, joinMemberCount={}",
                activity.getId(), activity.getOpenGroupCount(), activity.getJoinMemberCount());
    }

    /**
     * 批量回填活动统计数据。
     * <p>
     * 该方法用于活动列表场景统一补齐统计值，当前按活动逐个读取 Redis 统计 Hash，
     * 在保持实现简单的前提下复用单活动回填逻辑。
     *
     * @param activities 活动列表
     */
    public void fillActivityStatistics(List<GroupActivity> activities) {
        if (activities == null || activities.isEmpty()) {
            log.debug("批量回填活动统计跳过: activities为空");
            return;
        }
        log.debug("批量回填活动统计: activityCount={}", activities.size());
        for (GroupActivity activity : activities) {
            fillActivityStatistics(activity);
        }
        log.debug("批量回填活动统计完成: activityCount={}", activities.size());
    }

    /**
     * 将 Redis 统计数据应用到活动实体。
     *
     * @param activity 活动实体
     * @param statsMap Redis 统计Map
     */
    private void applyActivityStatistics(GroupActivity activity, Map<Object, Object> statsMap) {
        if (activity == null) {
            return;
        }
        activity.setOpenGroupCount(defaultLong(getLong(statsMap, FIELD_OPEN_GROUP_COUNT)));
        activity.setJoinMemberCount(defaultLong(getLong(statsMap, FIELD_JOIN_MEMBER_COUNT)));
    }

    private List<Object> executeMultiScript(String scriptName, List<String> keys, List<String> args) {
        log.debug("执行Lua脚本: scriptName={}, keyCount={}, argCount={}",
                scriptName, keys == null ? 0 : keys.size(), args == null ? 0 : args.size());
        List<Object> result = redisScriptComponent.executeLuaScript(scriptName, ReturnType.MULTI, keys, args);
        log.debug("执行Lua脚本完成: scriptName={}, resultSize={}, firstResult={}",
                scriptName,
                result == null ? 0 : result.size(),
                result == null || result.isEmpty() ? null : result.get(0));
        return result;
    }

    /**
     * 按订单号增量 upsert 成员轨迹。
     *
     * @param members 成员快照
     */
    private void upsertMembers(List<GroupMember> members) {
        if (members == null || members.isEmpty()) {
            log.debug("批量upsert成员轨迹跳过: members为空");
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
        log.debug("批量upsert成员轨迹完成: inputCount={}, upsertCount={}", members.size(), upsertCount);
    }

    /**
     * 读取成员快照。
     *
     * @param groupId 团ID
     * @return 成员列表
     */
    private List<GroupMember> loadMembers(String groupId) {
        log.debug("读取拼团成员快照: groupId={}", groupId);
        List<Object> memberJsonList = stringRedisTemplate.opsForHash()
                .values(groupRedisKeyBuilder.buildGroupMemberStoreKey(groupId));
        if (memberJsonList.isEmpty()) {
            log.debug("拼团成员快照为空: groupId={}", groupId);
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
        log.debug("读取拼团成员快照完成: groupId={}, memberCount={}", groupId, members.size());
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
        log.debug("构建拼团实例完成: groupId={}, status={}, currentSize={}, memberCount={}",
                groupId, instance.getStatus(), instance.getCurrentSize(), members.size());
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
            log.debug("构建拼团成员完成: groupId={}, userId={}, orderId={}, memberStatus={}",
                    member.getGroupInstanceId(), member.getUserId(), member.getOrderId(), member.getMemberStatus());
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
        log.debug("构建拼团成员摘要完成: userId={}, orderId={}, memberStatus={}",
                member.getUserId(), member.getOrderId(), member.getMemberStatus());
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
            log.warn("解析开团Lua结果失败: rawResult为空");
            return result;
        }
        int code = parseInt(rawResult.get(0));
        if (code == 1) {
            result.setSuccess(true);
            result.setGroupId(stringValue(rawResult, 1));
            log.debug("解析开团Lua结果完成: code={}, groupId={}, success={}, replayed={}",
                    code, result.getGroupId(), result.isSuccess(), result.isReplayed());
            return result;
        }
        if (code == 2) {
            result.setSuccess(true);
            result.setGroupId(stringValue(rawResult, 1));
            result.setReplayed(true);
            log.debug("解析开团Lua结果完成: code={}, groupId={}, success={}, replayed={}",
                    code, result.getGroupId(), result.isSuccess(), result.isReplayed());
            return result;
        }
        result.setFailReason(String.valueOf(code));
        log.debug("解析开团Lua结果完成: code={}, groupId={}, success={}, replayed={}, failReason={}",
                code, result.getGroupId(), result.isSuccess(), result.isReplayed(), result.getFailReason());
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
            log.warn("解析参团Lua结果失败: rawResult为空");
            return result;
        }
        int code = parseInt(rawResult.get(0));
        if (code == 1) {
            result.setSuccess(true);
            result.setGroupId(stringValue(rawResult, 1));
            result.setGroupStatus(stringValue(rawResult, 2));
            log.debug("解析参团Lua结果完成: code={}, groupId={}, success={}, replayed={}, groupStatus={}",
                    code, result.getGroupId(), result.isSuccess(), result.isReplayed(), result.getGroupStatus());
            return result;
        }
        if (code == 2) {
            result.setSuccess(true);
            result.setGroupId(stringValue(rawResult, 1));
            result.setReplayed(true);
            log.debug("解析参团Lua结果完成: code={}, groupId={}, success={}, replayed={}, groupStatus={}",
                    code, result.getGroupId(), result.isSuccess(), result.isReplayed(), result.getGroupStatus());
            return result;
        }
        result.setFailReason(String.valueOf(code));
        if (code == -4) {
            result.setGroupStatus(stringValue(rawResult, 1));
        } else {
            result.setGroupId(stringValue(rawResult, 1));
        }
        log.debug("解析参团Lua结果完成: code={}, groupId={}, success={}, replayed={}, groupStatus={}, failReason={}",
                code, result.getGroupId(), result.isSuccess(), result.isReplayed(), result.getGroupStatus(), result.getFailReason());
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
            log.warn("解析Lua状态码失败: rawResult为空");
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        int code = parseInt(rawResult.get(0));
        log.debug("解析Lua状态码完成: code={}", code);
        return code;
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
            String payload = JacksonUtils.toJsonString(member);
            log.debug("构建成员缓存载荷完成: groupId={}, userId={}, orderId={}, leader={}, memberStatus={}",
                    groupId, userId, orderId, leader, memberStatus);
            return payload;
        } catch (Exception e) {
            log.error("构建成员缓存载荷失败: groupId={}, userId={}, orderId={}", groupId, userId, orderId, e);
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
        long expireTime = nowMillis + activity.getExpireHours() * 3600_000L;
        log.debug("计算拼团过期时间: activityId={}, nowMillis={}, expireHours={}, expireTime={}",
                activity.getId(), nowMillis, activity.getExpireHours(), expireTime);
        return expireTime;
    }

    /**
     * 获取默认限购值。
     *
     * @param limitPerUser 限购值
     * @return 非空限购值
     */
    private int defaultLimitPerUser(Integer limitPerUser) {
        int limit = limitPerUser == null ? 0 : limitPerUser;
        log.debug("计算默认限购值: input={}, result={}", limitPerUser, limit);
        return limit;
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
        int result = leftValue.compareTo(rightValue);
        log.debug("比较时间完成: left={}, right={}, result={}", leftValue, rightValue, result);
        return result;
    }

    /**
     * 毫秒值转日期。
     *
     * @param millis 毫秒值
     * @return 日期
     */
    private Date toDate(Long millis) {
        Date date = millis == null || millis <= 0 ? null : new Date(millis);
        log.debug("毫秒值转日期完成: millis={}, date={}", millis, date);
        return date;
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
        String result = value == null ? null : String.valueOf(value);
        log.debug("读取字符串字段完成: key={}, value={}", key, result);
        return result;
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
        Long result = hasText(value) ? Long.parseLong(value) : null;
        log.debug("读取长整型字段完成: key={}, value={}", key, result);
        return result;
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
        Integer result = hasText(value) ? Integer.parseInt(value) : null;
        log.debug("读取整型字段完成: key={}, value={}", key, result);
        return result;
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
        Date result = millis == null || millis <= 0 ? null : new Date(millis);
        log.debug("读取日期字段完成: key={}, millis={}, value={}", key, millis, result);
        return result;
    }

    /**
     * 解析整型值。
     *
     * @param value 原值
     * @return 整型结果
     */
    private int parseInt(Object value) {
        int result = Integer.parseInt(String.valueOf(value));
        log.debug("解析整型值完成: rawValue={}, result={}", value, result);
        return result;
    }

    /**
     * 读取列表中的字符串项。
     *
     * @param values 返回列表
     * @param index 下标
     * @return 字符串值
     */
    private String stringValue(List<Object> values, int index) {
        String result = values.size() > index && values.get(index) != null ? String.valueOf(values.get(index)) : null;
        log.debug("读取Lua返回字符串项完成: index={}, value={}", index, result);
        return result;
    }

    /**
     * 空值保护。
     *
     * @param value 原值
     * @return 非 null 字符串
     */
    private String nullSafe(String value) {
        String result = value == null ? "" : value;
        log.debug("空值保护完成: originalNull={}, resultLength={}", value == null, result.length());
        return result;
    }

    /**
     * 长整型空值兜底。
     *
     * @param value 原始值
     * @return 非空长整型
     */
    private Long defaultLong(Long value) {
        Long result = value == null ? 0L : value;
        log.debug("长整型空值兜底完成: input={}, result={}", value, result);
        return result;
    }

    /**
     * 从 classpath 读取 Lua 脚本字节。
     *
     * @param path 资源路径
     * @return 脚本字节
     */
    private byte[] loadScriptBytes(String path) {
        try {
            byte[] bytes = StreamUtils.copyToByteArray(new ClassPathResource(path).getInputStream());
            log.debug("读取Lua脚本字节完成: path={}, byteSize={}", path, bytes.length);
            return bytes;
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
        boolean result = value != null && !value.trim().isEmpty();
        log.debug("判断文本是否有值完成: hasText={}", result);
        return result;
    }
}
