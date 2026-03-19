package com.ww.mall.promotion.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.app.common.exception.ApiException;
import com.ww.mall.promotion.constants.GroupBizConstants;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.engine.model.GroupCacheSnapshot;
import com.ww.mall.promotion.engine.model.GroupCommandResult;
import com.ww.mall.promotion.engine.model.GroupMemberCacheSnapshot;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.entity.group.GroupMember;
import com.ww.mall.promotion.enums.GroupEnabledStatus;
import com.ww.mall.promotion.enums.GroupMemberBizStatus;
import com.ww.mall.promotion.enums.GroupTradeType;
import com.ww.mall.promotion.key.GroupRedisKeyBuilder;
import com.ww.mall.promotion.mq.GroupAfterSaleSuccessMessage;
import com.ww.mall.promotion.mq.GroupOrderPaidMessage;
import com.ww.mall.promotion.service.group.command.CreateGroupCommand;
import com.ww.mall.promotion.service.group.command.JoinGroupCommand;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.ww.mall.promotion.constants.ErrorCodeConstants.*;

/**
 * 拼团命令服务。
 * <p>
 * 所有主链路状态迁移都通过 Redis Lua 脚本完成，Mongo 只由异步投影器负责落库。
 *
 * @author ww
 * @create 2026-03-19
 * @description: 拼团命令服务
 */
@Service
public class GroupCommandService {

    @Resource
    private LoadingCache<String, GroupActivity> groupActivityCache;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private GroupRedisKeyBuilder groupRedisKeyBuilder;

    @Resource
    private GroupQueryService groupQueryService;

    @Resource
    private GroupRedisStateReader groupRedisStateReader;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource(name = "groupCreateScript")
    private RedisScript<List<Object>> groupCreateScript;

    @Resource(name = "groupJoinScript")
    private RedisScript<List<Object>> groupJoinScript;

    @Resource(name = "groupAfterSaleScript")
    private RedisScript<List<Object>> groupAfterSaleScript;

    @Resource(name = "groupExpireScript")
    private RedisScript<List<Object>> groupExpireScript;

    /**
     * 创建拼团。
     *
     * @param command 开团命令
     * @return 团详情
     */
    public GroupInstanceVO createGroup(CreateGroupCommand command) {
        validateCreateCommand(command);
        GroupInstanceVO replayedGroup = replayCreateGroup(command);
        if (replayedGroup != null) {
            return replayedGroup;
        }
        GroupActivity activity = loadAndValidateActivity(command.getActivityId());
        resolveSkuRule(activity, command.getSkuId());
        Long userId = command.getUserId();
        String groupId = new ObjectId().toString();
        long nowMillis = System.currentTimeMillis();
        String activityUserCountField = groupRedisKeyBuilder.buildActivityUserCountField(activity.getId(), userId);
        List<String> keys = Arrays.asList(
                groupRedisKeyBuilder.buildGroupMetaKey(groupId),
                groupRedisKeyBuilder.buildGroupMemberStoreKey(groupId),
                groupRedisKeyBuilder.buildGroupUserIndexKey(groupId),
                groupRedisKeyBuilder.buildOrderIndexKey(),
                groupRedisKeyBuilder.buildActivityUserCountKey(),
                groupRedisKeyBuilder.buildExpiryIndexKey(),
                groupRedisKeyBuilder.buildDomainEventStreamKey()
        );
        List<String> args = Arrays.asList(
                groupId,
                activity.getId(),
                String.valueOf(userId),
                command.getOrderId(),
                String.valueOf(activity.getRequiredSize()),
                String.valueOf(activity.getSpuId()),
                buildMemberCachePayload(groupId, userId, command.getOrderId(), true,
                        nowMillis, command.getPayAmount(), command.getSkuId(),
                        GroupMemberBizStatus.JOINED.name(), null, "PAY_SUCCESS", nowMillis),
                String.valueOf(nowMillis),
                String.valueOf(buildExpireTime(nowMillis, activity)),
                String.valueOf(defaultLimitPerUser(activity.getLimitPerUser())),
                activityUserCountField,
                String.valueOf(GroupBizConstants.REDIS_GROUP_DATA_RETAIN_SECONDS)
        );
        List<Object> rawResult = stringRedisTemplate.execute(groupCreateScript, keys, args.toArray());
        GroupCommandResult result = parseCreateResult(rawResult);
        if (!result.isSuccess()) {
            throwCreateException(result);
        }
        return groupQueryService.getGroupDetail(result.getGroupId());
    }

    /**
     * 参团。
     *
     * @param command 参团命令
     * @return 团详情
     */
    public GroupInstanceVO joinGroup(JoinGroupCommand command) {
        validateJoinCommand(command);
        GroupInstanceVO replayedGroup = replayJoinGroup(command);
        if (replayedGroup != null) {
            return replayedGroup;
        }
        GroupCacheSnapshot snapshot = groupRedisStateReader.requireGroupSnapshot(command.getGroupId());
        GroupActivity activity = loadAndValidateActivity(snapshot.getInstance().getActivityId());
        resolveSkuRule(activity, command.getSkuId());
        Long userId = command.getUserId();
        long nowMillis = System.currentTimeMillis();
        String activityUserCountField = groupRedisKeyBuilder.buildActivityUserCountField(activity.getId(), userId);
        List<String> keys = Arrays.asList(
                groupRedisKeyBuilder.buildGroupMetaKey(command.getGroupId()),
                groupRedisKeyBuilder.buildGroupMemberStoreKey(command.getGroupId()),
                groupRedisKeyBuilder.buildGroupUserIndexKey(command.getGroupId()),
                groupRedisKeyBuilder.buildOrderIndexKey(),
                groupRedisKeyBuilder.buildActivityUserCountKey(),
                groupRedisKeyBuilder.buildExpiryIndexKey(),
                groupRedisKeyBuilder.buildDomainEventStreamKey()
        );
        List<String> args = Arrays.asList(
                command.getGroupId(),
                activity.getId(),
                String.valueOf(userId),
                command.getOrderId(),
                String.valueOf(defaultLimitPerUser(activity.getLimitPerUser())),
                buildMemberCachePayload(command.getGroupId(), userId, command.getOrderId(), false,
                        nowMillis, command.getPayAmount(), command.getSkuId(),
                        GroupMemberBizStatus.JOINED.name(), null, "PAY_SUCCESS", nowMillis),
                activityUserCountField,
                String.valueOf(nowMillis),
                String.valueOf(GroupBizConstants.REDIS_GROUP_DATA_RETAIN_SECONDS)
        );
        List<Object> rawResult = stringRedisTemplate.execute(groupJoinScript, keys, args.toArray());
        GroupCommandResult result = parseJoinResult(rawResult);
        if (!result.isSuccess()) {
            throwJoinException(result);
        }
        return groupQueryService.getGroupDetail(result.getGroupId());
    }

    /**
     * 处理支付成功消息。
     *
     * @param message 支付成功消息
     * @return 团详情
     */
    public GroupInstanceVO handleOrderPaid(GroupOrderPaidMessage message) {
        validatePaidMessage(message);
        if (message.getTradeType() == GroupTradeType.START) {
            CreateGroupCommand command = new CreateGroupCommand();
            command.setActivityId(message.getActivityId());
            command.setUserId(message.getUserId());
            command.setOrderId(message.getOrderId());
            command.setSkuId(message.getSkuId());
            command.setPayAmount(message.getPayAmount());
            return createGroup(command);
        }
        JoinGroupCommand command = new JoinGroupCommand();
        command.setGroupId(message.getGroupId());
        command.setUserId(message.getUserId());
        command.setOrderId(message.getOrderId());
        command.setSkuId(message.getSkuId());
        command.setPayAmount(message.getPayAmount());
        return joinGroup(command);
    }

    /**
     * 处理售后成功。
     *
     * @param message 售后消息
     */
    public void handleAfterSaleSuccess(GroupAfterSaleSuccessMessage message) {
        if (message == null || !hasText(message.getOrderId())) {
            throw new ApiException(GROUP_RECORD_ORDER_CODE_NOT_EXISTS);
        }
        String groupId = hasText(message.getGroupId()) ? message.getGroupId()
                : (String) stringRedisTemplate.opsForHash().get(groupRedisKeyBuilder.buildOrderIndexKey(), message.getOrderId());
        if (!hasText(groupId)) {
            GroupMember member = mongoTemplate.findOne(GroupMember.buildOrderIdQuery(message.getOrderId()), GroupMember.class);
            if (member != null) {
                groupId = member.getGroupInstanceId();
            }
        }
        if (!hasText(groupId)) {
            throw new ApiException(GROUP_RECORD_NOT_EXISTS);
        }
        GroupCacheSnapshot snapshot = groupRedisStateReader.requireGroupSnapshot(groupId);
        String activityId = snapshot.getInstance().getActivityId();
        Long userId = message.getUserId() != null ? message.getUserId() : findMemberUserId(snapshot, message.getOrderId());
        if (userId == null) {
            throw new ApiException(GROUP_RECORD_ORDER_CODE_NOT_EXISTS);
        }
        long nowMillis = message.getSuccessTime() != null ? message.getSuccessTime().getTime() : System.currentTimeMillis();
        List<String> keys = Arrays.asList(
                groupRedisKeyBuilder.buildGroupMetaKey(groupId),
                groupRedisKeyBuilder.buildGroupMemberStoreKey(groupId),
                groupRedisKeyBuilder.buildGroupUserIndexKey(groupId),
                groupRedisKeyBuilder.buildActivityUserCountKey(),
                groupRedisKeyBuilder.buildExpiryIndexKey(),
                groupRedisKeyBuilder.buildDomainEventStreamKey()
        );
        String failReason = userId != null && isLeader(snapshot, userId)
                ? "团长售后导致拼团关闭" : "售后成功，释放拼团名额";
        List<String> args = Arrays.asList(
                groupId,
                activityId,
                nullSafe(message.getAfterSaleId()),
                message.getOrderId(),
                String.valueOf(nowMillis),
                failReason,
                nullSafe(message.getReason()),
                String.valueOf(GroupBizConstants.REDIS_GROUP_DATA_RETAIN_SECONDS),
                groupRedisKeyBuilder.buildActivityUserCountFieldPrefix(activityId)
        );
        List<Object> rawResult = stringRedisTemplate.execute(groupAfterSaleScript, keys, args.toArray());
        if (rawResult == null || rawResult.isEmpty()) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        int code = parseInt(rawResult.get(0));
        if (code < 0) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
    }

    /**
     * 处理过期关团。
     *
     * @param groupId 团ID
     * @param reason 关团原因
     */
    public void expireGroup(String groupId, String reason) {
        GroupCacheSnapshot snapshot = groupRedisStateReader.requireGroupSnapshot(groupId);
        long nowMillis = System.currentTimeMillis();
        List<String> keys = Arrays.asList(
                groupRedisKeyBuilder.buildGroupMetaKey(groupId),
                groupRedisKeyBuilder.buildGroupMemberStoreKey(groupId),
                groupRedisKeyBuilder.buildGroupUserIndexKey(groupId),
                groupRedisKeyBuilder.buildActivityUserCountKey(),
                groupRedisKeyBuilder.buildDomainEventStreamKey(),
                groupRedisKeyBuilder.buildExpiryIndexKey()
        );
        List<String> args = Arrays.asList(
                groupId,
                reason,
                String.valueOf(nowMillis),
                String.valueOf(GroupBizConstants.REDIS_GROUP_DATA_RETAIN_SECONDS),
                groupRedisKeyBuilder.buildActivityUserCountFieldPrefix(snapshot.getInstance().getActivityId())
        );
        List<Object> rawResult = stringRedisTemplate.execute(groupExpireScript, keys, args.toArray());
        if (rawResult == null || rawResult.isEmpty()) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        int code = parseInt(rawResult.get(0));
        if (code < 0 && code != -1 && code != -2) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
    }

    /**
     * 加载并校验活动。
     *
     * @param activityId 活动ID
     * @return 活动对象
     */
    private GroupActivity loadAndValidateActivity(String activityId) {
        GroupActivity activity = groupActivityCache.get(activityId);
        if (activity == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        if (activity.getRequiredSize() == null || activity.getRequiredSize() <= 1) {
            throw new ApiException(GROUP_ACTIVITY_REQUIRED_SIZE_INVALID);
        }
        if (activity.getExpireHours() == null || activity.getExpireHours() <= 0) {
            throw new ApiException(GROUP_ACTIVITY_EXPIRE_HOURS_INVALID);
        }
        Date now = new Date();
        if (GroupEnabledStatus.DISABLED.getCode() == activity.getEnabled()) {
            throw new ApiException(GROUP_RECORD_FAILED_DISABLE);
        }
        if (activity.getStartTime() != null && activity.getStartTime().after(now)) {
            throw new ApiException(GROUP_RECORD_FAILED_TIME_NOT_START);
        }
        if (activity.getEndTime() != null && activity.getEndTime().before(now)) {
            throw new ApiException(GROUP_RECORD_FAILED_TIME_END);
        }
        return activity;
    }

    /**
     * 解析 SKU 规则。
     *
     * @param activity 活动
     * @param skuId SKU ID
     * @return SKU 规则
     */
    private GroupActivity.GroupSkuRule resolveSkuRule(GroupActivity activity, Long skuId) {
        if (activity == null || skuId == null) {
            throw new ApiException(GROUP_RECORD_SKU_NOT_SUPPORTED);
        }
        if (activity.getSkuRules() == null || activity.getSkuRules().isEmpty()) {
            if (activity.getSkuId() != null && activity.getSkuId().equals(skuId) && activity.getGroupPrice() != null) {
                GroupActivity.GroupSkuRule rule = new GroupActivity.GroupSkuRule();
                rule.setSkuId(activity.getSkuId());
                rule.setGroupPrice(activity.getGroupPrice());
                rule.setOriginalPrice(activity.getOriginalPrice());
                rule.setEnabled(GroupEnabledStatus.ENABLED.getCode());
                return rule;
            }
            throw new ApiException(GROUP_RECORD_SKU_NOT_SUPPORTED);
        }
        return activity.getSkuRules().stream()
                .filter(rule -> rule != null && skuId.equals(rule.getSkuId())
                        && GroupEnabledStatus.DISABLED.getCode() != rule.getEnabled())
                .findFirst()
                .orElseThrow(() -> new ApiException(GROUP_RECORD_SKU_NOT_SUPPORTED));
    }

    /**
     * 构建成员缓存JSON。
     *
     * @param groupId 团ID
     * @param userId 用户ID
     * @param orderId 订单ID
     * @param leader 是否团长
     * @param joinTime 入团时间
     * @param payAmount 支付金额
     * @param skuId SKU ID
     * @param memberStatus 成员业务状态
     * @param afterSaleId 售后单号
     * @param latestTrajectory 最近轨迹编码
     * @param latestTrajectoryTime 最近轨迹时间
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
            return objectMapper.writeValueAsString(member);
        } catch (Exception e) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
    }

    /**
     * 构建过期时间。
     *
     * @param nowMillis 当前时间
     * @param activity 活动
     * @return 过期毫秒值
     */
    private long buildExpireTime(long nowMillis, GroupActivity activity) {
        return nowMillis + activity.getExpireHours() * 3600_000L;
    }

    /**
     * 解析开团结果。
     *
     * @param rawResult 脚本返回值
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
     * 解析参团结果。
     *
     * @param rawResult 脚本返回值
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
     * 抛出开团异常。
     *
     * @param result 命令结果
     */
    private void throwCreateException(GroupCommandResult result) {
        if ("-3".equals(result.getFailReason())) {
            throw new ApiException(GROUP_RECORD_FAILED_HAVE_JOINED);
        }
        throw new ApiException(GROUP_CREATE_FAILED);
    }

    /**
     * 抛出参团异常。
     *
     * @param result 命令结果
     */
    private void throwJoinException(GroupCommandResult result) {
        if ("-1".equals(result.getFailReason())) {
            throw new ApiException(GROUP_RECORD_NOT_EXISTS);
        }
        if ("-2".equals(result.getFailReason())) {
            throw new ApiException(GROUP_RECORD_ORDER_DUPLICATED);
        }
        if ("-4".equals(result.getFailReason())) {
            if ("SUCCESS".equals(result.getGroupStatus())) {
                throw new ApiException(GROUP_RECORD_USER_FULL);
            }
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        if ("-5".equals(result.getFailReason())) {
            throw new ApiException(GROUP_RECORD_FAILED_TIME_END);
        }
        if ("-6".equals(result.getFailReason())) {
            throw new ApiException(GROUP_RECORD_EXISTS);
        }
        if ("-7".equals(result.getFailReason())) {
            throw new ApiException(GROUP_RECORD_FAILED_HAVE_JOINED);
        }
        if ("-8".equals(result.getFailReason())) {
            throw new ApiException(GROUP_RECORD_USER_FULL);
        }
        throw new ApiException(GROUP_RECORD_ERROR);
    }

    /**
     * 在正式开团前按订单进行幂等回放。
     * <p>
     * 优先读取 Redis 映射，映射丢失时再回落 Mongo 投影，避免 MQ 重投或缓存淘汰后重复建团。
     *
     * @param command 开团命令
     * @return 已存在的团详情，不存在时返回null
     */
    private GroupInstanceVO replayCreateGroup(CreateGroupCommand command) {
        String existingGroupId = findExistingGroupId(command.getOrderId());
        return hasText(existingGroupId) ? groupQueryService.getGroupDetail(existingGroupId) : null;
    }

    /**
     * 在正式参团前按订单进行幂等回放。
     *
     * @param command 参团命令
     * @return 已存在的团详情，不存在时返回null
     */
    private GroupInstanceVO replayJoinGroup(JoinGroupCommand command) {
        String existingGroupId = findExistingGroupId(command.getOrderId());
        if (!hasText(existingGroupId)) {
            return null;
        }
        if (!existingGroupId.equals(command.getGroupId())) {
            throw new ApiException(GROUP_RECORD_ORDER_DUPLICATED);
        }
        return groupQueryService.getGroupDetail(existingGroupId);
    }

    /**
     * 查询订单已归属的拼团ID。
     *
     * @param orderId 订单ID
     * @return 拼团ID
     */
    private String findExistingGroupId(String orderId) {
        String existingGroupId = (String) stringRedisTemplate.opsForHash().get(groupRedisKeyBuilder.buildOrderIndexKey(), orderId);
        if (hasText(existingGroupId)) {
            return existingGroupId;
        }
        GroupMember member = mongoTemplate.findOne(GroupMember.buildOrderIdQuery(orderId), GroupMember.class);
        return member == null ? null : member.getGroupInstanceId();
    }

    /**
     * 校验支付消息。
     *
     * @param message 支付消息
     */
    private void validatePaidMessage(GroupOrderPaidMessage message) {
        if (message == null || message.getTradeType() == null || !hasText(message.getOrderId())
                || message.getUserId() == null || message.getSkuId() == null || message.getPayAmount() == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        if (message.getTradeType() == GroupTradeType.START && !hasText(message.getActivityId())) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        if (message.getTradeType() == GroupTradeType.JOIN && !hasText(message.getGroupId())) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
    }

    /**
     * 校验开团命令。
     *
     * @param command 开团命令
     */
    private void validateCreateCommand(CreateGroupCommand command) {
        if (command == null || !hasText(command.getActivityId()) || !hasText(command.getOrderId())
                || command.getSkuId() == null || command.getUserId() == null || command.getPayAmount() == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
    }

    /**
     * 校验参团命令。
     *
     * @param command 参团命令
     */
    private void validateJoinCommand(JoinGroupCommand command) {
        if (command == null || !hasText(command.getGroupId()) || !hasText(command.getOrderId())
                || command.getSkuId() == null || command.getUserId() == null || command.getPayAmount() == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
    }

    /**
     * 查找成员用户ID。
     *
     * @param snapshot 团快照
     * @param orderId 订单ID
     * @return 用户ID
     */
    private Long findMemberUserId(GroupCacheSnapshot snapshot, String orderId) {
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
     * 判断是否团长。
     *
     * @param snapshot 团快照
     * @param userId 用户ID
     * @return true-团长
     */
    private boolean isLeader(GroupCacheSnapshot snapshot, Long userId) {
        return snapshot != null && snapshot.getInstance() != null
                && userId != null && userId.equals(snapshot.getInstance().getLeaderUserId());
    }

    /**
     * 默认限购值。
     *
     * @param limitPerUser 限购
     * @return 限购值
     */
    private int defaultLimitPerUser(Integer limitPerUser) {
        return limitPerUser == null ? 0 : limitPerUser;
    }

    /**
     * 解析整型。
     *
     * @param value 原值
     * @return 整型
     */
    private int parseInt(Object value) {
        return Integer.parseInt(String.valueOf(value));
    }

    /**
     * 读取脚本返回项。
     *
     * @param values 返回值
     * @param index 下标
     * @return 文本
     */
    private String stringValue(List<Object> values, int index) {
        return values.size() > index && values.get(index) != null ? String.valueOf(values.get(index)) : null;
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

    /**
     * 空值保护。
     *
     * @param value 原值
     * @return 非null值
     */
    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
