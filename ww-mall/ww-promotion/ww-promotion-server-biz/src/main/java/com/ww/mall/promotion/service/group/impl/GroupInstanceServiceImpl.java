package com.ww.mall.promotion.service.group.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.common.exception.ApiException;
import com.ww.app.rabbitmq.RabbitMqPublisher;
import com.ww.app.redis.component.RedissonComponent;
import com.ww.mall.promotion.constants.GroupBizConstants;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.entity.group.GroupInstance;
import com.ww.mall.promotion.entity.group.GroupMember;
import com.ww.mall.promotion.enums.*;
import com.ww.mall.promotion.key.GroupRedisKeyBuilder;
import com.ww.mall.promotion.mq.*;
import com.ww.mall.promotion.service.group.GroupInstanceService;
import com.ww.mall.promotion.service.group.command.CreateGroupCommand;
import com.ww.mall.promotion.service.group.command.JoinGroupCommand;
import com.ww.mall.promotion.service.group.support.GroupFlowLogSupport;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.redisson.api.RLock;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.ww.mall.promotion.constants.ErrorCodeConstants.*;

/**
 * 拼团实例服务实现。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 新版拼团核心服务
 */
@Slf4j
@Service
public class GroupInstanceServiceImpl implements GroupInstanceService {

    private static final String SUMMARY_FIELD_ACTIVITY_ID = "activityId";
    private static final String SUMMARY_FIELD_LEADER_USER_ID = "leaderUserId";
    private static final String SUMMARY_FIELD_STATUS = "status";
    private static final String SUMMARY_FIELD_REQUIRED_SIZE = "requiredSize";
    private static final String SUMMARY_FIELD_CURRENT_SIZE = "currentSize";
    private static final String SUMMARY_FIELD_REMAINING_SLOTS = "remainingSlots";
    private static final String SUMMARY_FIELD_EXPIRE_TIME = "expireTime";
    private static final String SUMMARY_FIELD_COMPLETE_TIME = "completeTime";
    private static final String SUMMARY_FIELD_FAILED_TIME = "failedTime";
    private static final String SUMMARY_FIELD_GROUP_PRICE = "groupPrice";
    private static final String SUMMARY_FIELD_SPU_ID = "spuId";
    private static final String SUMMARY_FIELD_SKU_IDS = "skuIds";
    private static final String SUMMARY_FIELD_FAIL_REASON = "failReason";
    private static final String TRAJECTORY_PAY_SUCCESS = "PAY_SUCCESS";
    private static final String TRAJECTORY_GROUP_SUCCESS = "GROUP_SUCCESS";
    private static final String TRAJECTORY_GROUP_FAILED = "GROUP_FAILED";
    private static final String TRAJECTORY_AFTER_SALE = "AFTER_SALE_SUCCESS";
    private static final String TRAJECTORY_SEAT_RELEASED = "SEAT_RELEASED";

    @Resource
    private LoadingCache<String, GroupActivity> groupActivityCache;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private GroupRedisKeyBuilder groupRedisKeyBuilder;

    @Resource
    private RabbitMqPublisher rabbitMqPublisher;

    @Resource
    private GroupFlowLogSupport groupFlowLogSupport;

    @Resource
    private RedissonComponent redissonComponent;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public GroupInstanceVO createGroup(CreateGroupCommand command) {
        validateCreateCommand(command);
        Long userId = resolveUserId(command.getUserId());
        String traceId = groupFlowLogSupport.createTraceId();
        groupFlowLogSupport.record(traceId, null, command.getActivityId(), userId, command.getOrderId(),
                GroupFlowStage.CREATE_GROUP, GroupFlowSource.GROUP_SERVICE, GroupFlowStatus.PROCESSING,
                null, null, command);
        try {
            GroupInstanceVO result = executeUnderLock(groupRedisKeyBuilder.buildTradeLockKey(command.getOrderId()),
                    () -> doCreateGroup(command, userId));
            groupFlowLogSupport.record(traceId, result.getId(), result.getActivityId(), userId, command.getOrderId(),
                    GroupFlowStage.CREATE_GROUP, GroupFlowSource.GROUP_SERVICE, GroupFlowStatus.SUCCESS,
                    null, null, command);
            return result;
        } catch (ApiException e) {
            groupFlowLogSupport.recordFailure(traceId, null, command.getActivityId(), userId, command.getOrderId(),
                    GroupFlowStage.CREATE_GROUP, GroupFlowSource.GROUP_SERVICE, e.getMessage(), command);
            throw e;
        } catch (Exception e) {
            groupFlowLogSupport.recordFailure(traceId, null, command.getActivityId(), userId, command.getOrderId(),
                    GroupFlowStage.CREATE_GROUP, GroupFlowSource.GROUP_SERVICE, e.getMessage(), command);
            throw new ApiException(GROUP_CREATE_FAILED.getMsg() + ": " + e.getMessage());
        }
    }

    @Override
    public GroupInstanceVO joinGroup(JoinGroupCommand command) {
        validateJoinCommand(command);
        Long userId = resolveUserId(command.getUserId());
        LoadedGroupSnapshot snapshot = loadGroupSnapshot(command.getGroupId(), true);
        if (snapshot == null) {
            throw new ApiException(GROUP_RECORD_NOT_EXISTS);
        }
        String traceId = groupFlowLogSupport.createTraceId();
        groupFlowLogSupport.record(traceId, command.getGroupId(), snapshot.instance.getActivityId(), userId, command.getOrderId(),
                GroupFlowStage.JOIN_GROUP, GroupFlowSource.GROUP_SERVICE, GroupFlowStatus.PROCESSING,
                null, null, command);
        try {
            GroupInstanceVO result = executeUnderLock(groupRedisKeyBuilder.buildGroupLockKey(command.getGroupId()),
                    () -> doJoinGroup(command, userId));
            groupFlowLogSupport.record(traceId, result.getId(), result.getActivityId(), userId, command.getOrderId(),
                    GroupFlowStage.JOIN_GROUP, GroupFlowSource.GROUP_SERVICE, GroupFlowStatus.SUCCESS,
                    null, null, command);
            return result;
        } catch (ApiException e) {
            groupFlowLogSupport.recordFailure(traceId, command.getGroupId(), snapshot.instance.getActivityId(), userId,
                    command.getOrderId(), GroupFlowStage.JOIN_GROUP, GroupFlowSource.GROUP_SERVICE, e.getMessage(), command);
            throw e;
        } catch (Exception e) {
            groupFlowLogSupport.recordFailure(traceId, command.getGroupId(), snapshot.instance.getActivityId(), userId,
                    command.getOrderId(), GroupFlowStage.JOIN_GROUP, GroupFlowSource.GROUP_SERVICE, e.getMessage(), command);
            throw new ApiException(GROUP_RECORD_ERROR.getMsg() + ": " + e.getMessage());
        }
    }

    @Override
    public GroupInstanceVO getGroupDetail(String groupId) {
        if (!hasText(groupId)) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        LoadedGroupSnapshot snapshot = loadGroupSnapshot(groupId, true);
        if (snapshot == null) {
            throw new ApiException(GROUP_RECORD_NOT_EXISTS);
        }
        return convertToVO(snapshot.instance, snapshot.members);
    }

    @Override
    public List<GroupInstanceVO> getUserGroups() {
        Long userId = AuthorizationContext.getClientUser() != null ? AuthorizationContext.getClientUser().getId() : null;
        if (userId == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        Set<String> groupIds = stringRedisTemplate.opsForSet().members(groupRedisKeyBuilder.buildUserGroupKey(userId));
        if (groupIds == null || groupIds.isEmpty()) {
            Query query = new Query().addCriteria(Criteria.where("userId").is(userId));
            groupIds = mongoTemplate.find(query, GroupMember.class).stream()
                    .map(GroupMember::getGroupInstanceId)
                    .filter(this::hasText)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (groupIds == null || groupIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<GroupInstanceVO> result = new ArrayList<>();
        for (String groupId : groupIds) {
            try {
                result.add(getGroupDetail(groupId));
            } catch (Exception e) {
                log.warn("加载用户拼团详情失败: userId={}, groupId={}", userId, groupId, e);
            }
        }
        return result;
    }

    @Override
    public List<GroupInstanceVO> getActivityGroups(String activityId, String status) {
        if (!hasText(activityId)) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        return mongoTemplate.find(GroupInstance.buildActivityIdAndStatusQuery(activityId, status), GroupInstance.class)
                .stream()
                .map(instance -> convertToVO(instance,
                        mongoTemplate.find(GroupMember.buildGroupInstanceIdQuery(instance.getId()), GroupMember.class)))
                .collect(Collectors.toList());
    }

    @Override
    public void handleGroupSuccess(String groupId) {
        executeUnderLock(groupRedisKeyBuilder.buildGroupLockKey(groupId), () -> {
            LoadedGroupSnapshot snapshot = requireSnapshot(groupId);
            if (!GroupStatus.SUCCESS.getCode().equals(snapshot.instance.getStatus())
                    && countActiveMembers(snapshot.members) >= snapshot.instance.getRequiredSize()) {
                completeGroup(snapshot, groupFlowLogSupport.createTraceId(), GroupFlowSource.GROUP_SERVICE);
            }
            return null;
        });
    }

    @Override
    public void handleGroupFailed(String groupId) {
        executeUnderLock(groupRedisKeyBuilder.buildGroupLockKey(groupId), () -> {
            LoadedGroupSnapshot snapshot = requireSnapshot(groupId);
            if (GroupStatus.OPEN.getCode().equals(snapshot.instance.getStatus())) {
                failGroup(snapshot, groupFlowLogSupport.createTraceId(), GroupFlowSource.GROUP_JOB, "拼团过期未成团");
            }
            return null;
        });
    }

    @Override
    public void handleAfterSaleSuccess(GroupAfterSaleSuccessMessage message) {
        if (message == null || !hasText(message.getOrderId())) {
            throw new ApiException(GROUP_RECORD_ORDER_CODE_NOT_EXISTS);
        }
        String traceId = hasText(message.getTraceId()) ? message.getTraceId() : groupFlowLogSupport.createTraceId();
        groupFlowLogSupport.record(traceId, message.getGroupId(), null, message.getUserId(), message.getOrderId(),
                GroupFlowStage.AFTER_SALE_MQ, GroupFlowSource.GROUP_MQ_CONSUMER, GroupFlowStatus.PROCESSING,
                null, null, message);
        executeUnderLock(groupRedisKeyBuilder.buildAfterSaleLockKey(
                hasText(message.getAfterSaleId()) ? message.getAfterSaleId() : message.getOrderId()), () -> {
            doHandleAfterSale(message, traceId);
            return null;
        });
        groupFlowLogSupport.record(traceId, message.getGroupId(), null, message.getUserId(), message.getOrderId(),
                GroupFlowStage.AFTER_SALE_MQ, GroupFlowSource.GROUP_MQ_CONSUMER, GroupFlowStatus.SUCCESS,
                null, null, message);
    }

    /**
     * 执行开团。
     */
    private GroupInstanceVO doCreateGroup(CreateGroupCommand command, Long userId) {
        String mappedGroupId = stringRedisTemplate.opsForValue().get(groupRedisKeyBuilder.buildOrderMappingKey(command.getOrderId()));
        if (hasText(mappedGroupId)) {
            return getGroupDetail(mappedGroupId);
        }
        GroupActivity activity = loadAndValidateActivity(command.getActivityId());
        GroupActivity.GroupSkuRule skuRule = resolveSkuRule(activity, command.getSkuId());
        checkUserLimit(activity.getId(), userId, activity.getLimitPerUser());
        reserveActivityStock(activity.getId(), 1);
        boolean success = false;
        try {
            Date now = new Date();
            String groupId = new ObjectId().toString();
            GroupMember leader = buildMember(groupId, activity, skuRule, command.getOrderId(), userId, true, now);
            appendTrajectory(leader, TRAJECTORY_PAY_SUCCESS, "支付成功，开团占位成功",
                    GroupFlowSource.GROUP_SERVICE.name(), null, now);
            GroupInstance instance = buildInstance(groupId, activity, leader, now);
            persistSnapshot(instance, Collections.singletonList(leader));
            cacheSnapshot(instance, Collections.singletonList(leader));
            success = true;
            return convertToVO(instance, Collections.singletonList(leader));
        } finally {
            if (!success) {
                releaseActivityStock(activity.getId(), 1);
            }
        }
    }

    /**
     * 执行参团。
     */
    private GroupInstanceVO doJoinGroup(JoinGroupCommand command, Long userId) {
        LoadedGroupSnapshot snapshot = requireSnapshot(command.getGroupId());
        if (!GroupStatus.OPEN.getCode().equals(snapshot.instance.getStatus())) {
            throw new ApiException(GROUP_RECORD_USER_FULL);
        }
        if (snapshot.instance.getExpireTime() != null && snapshot.instance.getExpireTime().before(new Date())) {
            failGroup(snapshot, groupFlowLogSupport.createTraceId(), GroupFlowSource.GROUP_SERVICE, "拼团已过期");
            throw new ApiException(GROUP_RECORD_FAILED_TIME_END);
        }
        if (snapshot.members.stream().anyMatch(item -> item.getUserId() != null && item.getUserId().equals(userId)
                && GroupMemberBizStatus.activeStatuses().contains(item.getMemberStatus()))) {
            throw new ApiException(GROUP_RECORD_EXISTS);
        }
        if (snapshot.members.stream().anyMatch(item -> hasText(item.getOrderId())
                && item.getOrderId().equals(command.getOrderId()))) {
            throw new ApiException(GROUP_RECORD_ORDER_DUPLICATED);
        }
        if (snapshot.instance.getRemainingSlots() != null && snapshot.instance.getRemainingSlots() <= 0) {
            throw new ApiException(GROUP_RECORD_USER_FULL);
        }
        GroupActivity activity = loadAndValidateActivity(snapshot.instance.getActivityId());
        GroupActivity.GroupSkuRule skuRule = resolveSkuRule(activity, command.getSkuId());
        checkUserLimit(activity.getId(), userId, activity.getLimitPerUser());
        reserveActivityStock(activity.getId(), 1);
        boolean success = false;
        try {
            Date now = new Date();
            GroupMember member = buildMember(snapshot.instance.getId(), activity, skuRule, command.getOrderId(), userId, false, now);
            appendTrajectory(member, TRAJECTORY_PAY_SUCCESS, "支付成功，参团占位成功",
                    GroupFlowSource.GROUP_SERVICE.name(), null, now);
            snapshot.members.add(member);
            updateInstanceAggregate(snapshot.instance, snapshot.members);
            if (countActiveMembers(snapshot.members) >= snapshot.instance.getRequiredSize()) {
                completeGroup(snapshot, groupFlowLogSupport.createTraceId(), GroupFlowSource.GROUP_SERVICE);
            } else {
                persistSnapshot(snapshot.instance, snapshot.members);
                cacheSnapshot(snapshot.instance, snapshot.members);
            }
            success = true;
            return convertToVO(snapshot.instance, snapshot.members);
        } finally {
            if (!success) {
                releaseActivityStock(activity.getId(), 1);
            }
        }
    }

    /**
     * 处理售后成功。
     */
    private void doHandleAfterSale(GroupAfterSaleSuccessMessage message, String traceId) {
        GroupMember member = mongoTemplate.findOne(GroupMember.buildOrderIdQuery(message.getOrderId()), GroupMember.class);
        if (member == null) {
            log.warn("售后消息未匹配到拼团成员: orderId={}", message.getOrderId());
            return;
        }
        LoadedGroupSnapshot snapshot = requireSnapshot(member.getGroupInstanceId());
        GroupMember targetMember = snapshot.members.stream()
                .filter(item -> hasText(item.getOrderId()) && item.getOrderId().equals(message.getOrderId()))
                .findFirst()
                .orElse(member);
        Date now = message.getSuccessTime() != null ? message.getSuccessTime() : new Date();
        if (GroupMemberBizStatus.AFTER_SALE_RELEASED.name().equals(targetMember.getMemberStatus())
                || GroupMemberBizStatus.LEADER_AFTER_SALE_CLOSED.name().equals(targetMember.getMemberStatus())) {
            return;
        }
        appendTrajectory(targetMember, TRAJECTORY_AFTER_SALE, "售后成功，进入拼团售后处理",
                GroupFlowSource.GROUP_MQ_CONSUMER.name(), message.getReason(), now);
        targetMember.setAfterSaleId(message.getAfterSaleId());
        targetMember.setAfterSaleTime(now);
        if (GroupStatus.SUCCESS.getCode().equals(snapshot.instance.getStatus())) {
            persistSnapshot(snapshot.instance, snapshot.members);
            cacheSnapshot(snapshot.instance, snapshot.members);
            return;
        }
        if (targetMember.getIsLeader() != null && targetMember.getIsLeader() == 1) {
            targetMember.setMemberStatus(GroupMemberBizStatus.LEADER_AFTER_SALE_CLOSED.name());
            targetMember.setReleaseTime(now);
            targetMember.setStatus(GroupMemberStatus.EXITED.getCode());
            appendTrajectory(targetMember, TRAJECTORY_SEAT_RELEASED, "团长售后成功，未成团的团直接关闭",
                    GroupFlowSource.GROUP_MQ_CONSUMER.name(), message.getReason(), now);
            failGroup(snapshot, traceId, GroupFlowSource.GROUP_MQ_CONSUMER, "团长售后导致拼团关闭");
            return;
        }
        targetMember.setMemberStatus(GroupMemberBizStatus.AFTER_SALE_RELEASED.name());
        targetMember.setReleaseTime(now);
        targetMember.setStatus(GroupMemberStatus.EXITED.getCode());
        appendTrajectory(targetMember, TRAJECTORY_SEAT_RELEASED, "售后成功，未成团，归还拼团名额",
                GroupFlowSource.GROUP_MQ_CONSUMER.name(), message.getReason(), now);
        releaseActivityStock(snapshot.instance.getActivityId(), 1);
        updateInstanceAggregate(snapshot.instance, snapshot.members);
        persistSnapshot(snapshot.instance, snapshot.members);
        cacheSnapshot(snapshot.instance, snapshot.members);
        groupFlowLogSupport.record(traceId, snapshot.instance.getId(), snapshot.instance.getActivityId(), targetMember.getUserId(),
                targetMember.getOrderId(), GroupFlowStage.MEMBER_RELEASE, GroupFlowSource.GROUP_MQ_CONSUMER,
                GroupFlowStatus.SUCCESS, null, message.getReason(), message);
    }

    /**
     * 成团。
     */
    private void completeGroup(LoadedGroupSnapshot snapshot, String traceId, GroupFlowSource source) {
        Date now = new Date();
        snapshot.instance.setStatus(GroupStatus.SUCCESS.getCode());
        snapshot.instance.setCompleteTime(now);
        snapshot.instance.setFailReason(null);
        for (GroupMember member : snapshot.members) {
            if (GroupMemberBizStatus.JOINED.name().equals(member.getMemberStatus())) {
                member.setMemberStatus(GroupMemberBizStatus.SUCCESS.name());
                member.setSuccessTime(now);
                appendTrajectory(member, TRAJECTORY_GROUP_SUCCESS, "拼团成功", source.name(), null, now);
            }
        }
        persistSnapshot(snapshot.instance, snapshot.members);
        cacheSnapshot(snapshot.instance, snapshot.members);
        publishGroupSuccess(traceId, snapshot.instance, snapshot.members);
        groupFlowLogSupport.record(traceId, snapshot.instance.getId(), snapshot.instance.getActivityId(), null, null,
                GroupFlowStage.GROUP_SUCCESS, source, GroupFlowStatus.SUCCESS, null, null, snapshot.instance);
    }

    /**
     * 失败关团。
     */
    private void failGroup(LoadedGroupSnapshot snapshot, String traceId, GroupFlowSource source, String reason) {
        Date now = new Date();
        snapshot.instance.setStatus(GroupStatus.FAILED.getCode());
        snapshot.instance.setFailedTime(now);
        snapshot.instance.setFailReason(reason);
        List<GroupRefundMessage.RefundOrder> refundOrders = new ArrayList<>();
        int releaseCount = 0;
        for (GroupMember member : snapshot.members) {
            if (GroupMemberBizStatus.LEADER_AFTER_SALE_CLOSED.name().equals(member.getMemberStatus())) {
                releaseCount++;
                appendTrajectory(member, TRAJECTORY_GROUP_FAILED, "团长售后触发关团", source.name(), reason, now);
                continue;
            }
            if (GroupMemberBizStatus.JOINED.name().equals(member.getMemberStatus())
                    || GroupMemberBizStatus.SUCCESS.name().equals(member.getMemberStatus())) {
                member.setMemberStatus(GroupMemberBizStatus.FAILED_REFUND_PENDING.name());
                member.setStatus(GroupMemberStatus.EXITED.getCode());
                member.setReleaseTime(now);
                appendTrajectory(member, TRAJECTORY_GROUP_FAILED, "拼团失败，等待退款", source.name(), reason, now);
                refundOrders.add(buildRefundOrder(member));
                releaseCount++;
            }
        }
        if (releaseCount > 0) {
            releaseActivityStock(snapshot.instance.getActivityId(), releaseCount);
        }
        persistSnapshot(snapshot.instance, snapshot.members);
        cacheSnapshot(snapshot.instance, snapshot.members);
        publishGroupFailed(traceId, snapshot.instance, reason);
        if (!refundOrders.isEmpty()) {
            publishRefund(traceId, snapshot.instance, reason, refundOrders);
        }
        groupFlowLogSupport.record(traceId, snapshot.instance.getId(), snapshot.instance.getActivityId(), null, null,
                GroupFlowStage.GROUP_FAILED, source, GroupFlowStatus.SUCCESS, null, reason, snapshot.instance);
    }

    /**
     * 构建团实例。
     */
    private GroupInstance buildInstance(String groupId, GroupActivity activity, GroupMember leader, Date now) {
        GroupInstance instance = new GroupInstance();
        instance.setId(groupId);
        instance.setActivityId(activity.getId());
        instance.setLeaderUserId(leader.getUserId());
        instance.setStatus(GroupStatus.OPEN.getCode());
        instance.setRequiredSize(activity.getRequiredSize());
        instance.setCurrentSize(1);
        instance.setRemainingSlots(Math.max(0, activity.getRequiredSize() - 1));
        instance.setExpireTime(new Date(now.getTime() + activity.getExpireHours() * 3600_000L));
        instance.setGroupPrice(leader.getGroupPrice());
        instance.setSpuId(activity.getSpuId());
        instance.setSkuId(leader.getSkuId());
        instance.setSkuIds(Collections.singletonList(leader.getSkuId()));
        instance.setMembers(Collections.singletonList(buildMemberSummary(leader)));
        instance.setCreateTime(now);
        instance.setUpdateTime(now);
        return instance;
    }

    /**
     * 构建成员。
     */
    private GroupMember buildMember(String groupId, GroupActivity activity, GroupActivity.GroupSkuRule skuRule,
                                    String orderId, Long userId, boolean leader, Date now) {
        GroupMember member = new GroupMember();
        member.setGroupInstanceId(groupId);
        member.setActivityId(activity.getId());
        member.setUserId(userId);
        member.setOrderId(orderId);
        member.setIsLeader(leader ? 1 : 0);
        member.setJoinTime(now);
        member.setGroupPrice(skuRule.getGroupPrice());
        member.setSpuId(activity.getSpuId());
        member.setSkuId(skuRule.getSkuId());
        member.setStatus(GroupMemberStatus.NORMAL.getCode());
        member.setMemberStatus(GroupMemberBizStatus.JOINED.name());
        member.setCreateTime(now);
        member.setUpdateTime(now);
        return member;
    }

    /**
     * 更新团聚合。
     */
    private void updateInstanceAggregate(GroupInstance instance, List<GroupMember> members) {
        int currentSize = countActiveMembers(members);
        instance.setCurrentSize(currentSize);
        instance.setRemainingSlots(Math.max(0, instance.getRequiredSize() - currentSize));
        instance.setSkuId(members.stream().map(GroupMember::getSkuId).filter(item -> item != null).findFirst().orElse(null));
        instance.setSkuIds(members.stream().map(GroupMember::getSkuId).filter(item -> item != null)
                .distinct().collect(Collectors.toList()));
        instance.setMembers(members.stream()
                .sorted((left, right) -> compareDate(left.getJoinTime(), right.getJoinTime()))
                .map(this::buildMemberSummary)
                .collect(Collectors.toList()));
        instance.setUpdateTime(new Date());
    }

    /**
     * 持久化快照。
     */
    private void persistSnapshot(GroupInstance instance, List<GroupMember> members) {
        updateInstanceAggregate(instance, members);
        mongoTemplate.save(instance);
        for (GroupMember member : members) {
            member.setUpdateTime(new Date());
            mongoTemplate.save(member);
        }
    }

    /**
     * 缓存快照。
     */
    private void cacheSnapshot(GroupInstance instance, List<GroupMember> members) {
        try {
            String metaKey = groupRedisKeyBuilder.buildGroupMetaKey(instance.getId());
            String memberSortKey = groupRedisKeyBuilder.buildGroupMembersKey(instance.getId());
            String memberDetailKey = groupRedisKeyBuilder.buildGroupMemberDetailKey(instance.getId());
            stringRedisTemplate.delete(Arrays.asList(metaKey, memberSortKey, memberDetailKey));
            stringRedisTemplate.opsForHash().putAll(metaKey, buildSummaryMap(instance));
            long ttlSeconds = GroupBizConstants.REDIS_GROUP_DATA_RETAIN_SECONDS;
            stringRedisTemplate.expire(metaKey, ttlSeconds, TimeUnit.SECONDS);
            stringRedisTemplate.expire(memberSortKey, ttlSeconds, TimeUnit.SECONDS);
            stringRedisTemplate.expire(memberDetailKey, ttlSeconds, TimeUnit.SECONDS);
            for (GroupMember member : members) {
                double score = member.getJoinTime() != null ? member.getJoinTime().getTime() : System.currentTimeMillis();
                stringRedisTemplate.opsForZSet().add(memberSortKey, String.valueOf(member.getUserId()), score);
                stringRedisTemplate.opsForHash().put(memberDetailKey, String.valueOf(member.getUserId()), toJson(member));
                stringRedisTemplate.opsForValue().set(groupRedisKeyBuilder.buildOrderMappingKey(member.getOrderId()),
                        instance.getId(), ttlSeconds, TimeUnit.SECONDS);
                if (member.getUserId() != null) {
                    String userGroupKey = groupRedisKeyBuilder.buildUserGroupKey(member.getUserId());
                    stringRedisTemplate.opsForSet().add(userGroupKey, instance.getId());
                    stringRedisTemplate.expire(userGroupKey, ttlSeconds, TimeUnit.SECONDS);
                }
            }
            if (GroupStatus.OPEN.getCode().equals(instance.getStatus()) && instance.getExpireTime() != null) {
                stringRedisTemplate.opsForZSet().add(groupRedisKeyBuilder.buildExpiryIndexKey(),
                        instance.getId(), instance.getExpireTime().getTime());
                stringRedisTemplate.opsForZSet().add(groupRedisKeyBuilder.buildActivityOpenGroupKey(instance.getActivityId()),
                        instance.getId(), instance.getExpireTime().getTime());
            } else {
                stringRedisTemplate.opsForZSet().remove(groupRedisKeyBuilder.buildExpiryIndexKey(), instance.getId());
                stringRedisTemplate.opsForZSet().remove(groupRedisKeyBuilder.buildActivityOpenGroupKey(instance.getActivityId()),
                        instance.getId());
            }
        } catch (Exception e) {
            log.error("缓存拼团快照失败: groupId={}", instance.getId(), e);
        }
    }

    /**
     * 加载快照。
     */
    private LoadedGroupSnapshot loadGroupSnapshot(String groupId, boolean rebuildOnMongoHit) {
        Map<Object, Object> meta = stringRedisTemplate.opsForHash().entries(groupRedisKeyBuilder.buildGroupMetaKey(groupId));
        if (meta != null && !meta.isEmpty()) {
            GroupInstance instance = parseInstance(groupId, meta);
            if (instance != null) {
                return new LoadedGroupSnapshot(instance, parseMembers(groupId));
            }
        }
        GroupInstance instance = mongoTemplate.findOne(GroupInstance.buildIdQuery(groupId), GroupInstance.class);
        if (instance == null) {
            return null;
        }
        List<GroupMember> members = mongoTemplate.find(GroupMember.buildGroupInstanceIdQuery(groupId), GroupMember.class);
        LoadedGroupSnapshot snapshot = new LoadedGroupSnapshot(instance, members);
        if (rebuildOnMongoHit) {
            cacheSnapshot(instance, members);
        }
        return snapshot;
    }

    /**
     * 解析Redis摘要。
     */
    private GroupInstance parseInstance(String groupId, Map<Object, Object> meta) {
        try {
            GroupInstance instance = new GroupInstance();
            instance.setId(groupId);
            instance.setActivityId(getString(meta, SUMMARY_FIELD_ACTIVITY_ID));
            instance.setLeaderUserId(getLong(meta, SUMMARY_FIELD_LEADER_USER_ID));
            instance.setStatus(getString(meta, SUMMARY_FIELD_STATUS));
            instance.setRequiredSize(getInteger(meta, SUMMARY_FIELD_REQUIRED_SIZE));
            instance.setCurrentSize(getInteger(meta, SUMMARY_FIELD_CURRENT_SIZE));
            instance.setRemainingSlots(getInteger(meta, SUMMARY_FIELD_REMAINING_SLOTS));
            instance.setExpireTime(getDate(meta, SUMMARY_FIELD_EXPIRE_TIME));
            instance.setCompleteTime(getDate(meta, SUMMARY_FIELD_COMPLETE_TIME));
            instance.setFailedTime(getDate(meta, SUMMARY_FIELD_FAILED_TIME));
            instance.setGroupPrice(getDecimal(meta, SUMMARY_FIELD_GROUP_PRICE));
            instance.setSpuId(getLong(meta, SUMMARY_FIELD_SPU_ID));
            String skuIdsJson = getString(meta, SUMMARY_FIELD_SKU_IDS);
            if (hasText(skuIdsJson)) {
                instance.setSkuIds(objectMapper.readValue(skuIdsJson, new TypeReference<List<Long>>() { }));
                if (instance.getSkuIds() != null && !instance.getSkuIds().isEmpty()) {
                    instance.setSkuId(instance.getSkuIds().get(0));
                }
            }
            instance.setFailReason(getString(meta, SUMMARY_FIELD_FAIL_REASON));
            return instance;
        } catch (Exception e) {
            log.error("解析拼团摘要失败: groupId={}", groupId, e);
            return null;
        }
    }

    /**
     * 解析成员缓存。
     */
    private List<GroupMember> parseMembers(String groupId) {
        Map<Object, Object> detailMap = stringRedisTemplate.opsForHash().entries(groupRedisKeyBuilder.buildGroupMemberDetailKey(groupId));
        if (detailMap == null || detailMap.isEmpty()) {
            return new ArrayList<>();
        }
        List<GroupMember> members = new ArrayList<>();
        for (Object value : detailMap.values()) {
            try {
                members.add(objectMapper.readValue(String.valueOf(value), GroupMember.class));
            } catch (Exception e) {
                log.warn("解析拼团成员缓存失败: groupId={}", groupId, e);
            }
        }
        members.sort((left, right) -> compareDate(left.getJoinTime(), right.getJoinTime()));
        return members;
    }

    /**
     * 加载并校验活动。
     */
    private GroupActivity loadAndValidateActivity(String activityId) {
        GroupActivity activity = groupActivityCache.get(activityId);
        if (activity == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
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
     * 解析SKU规则。
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
     * 校验限购。
     */
    private void checkUserLimit(String activityId, Long userId, Integer limitPerUser) {
        if (limitPerUser == null || limitPerUser <= 0) {
            return;
        }
        Query query = GroupMember.buildUserIdAndActivityIdQuery(userId, activityId, GroupMemberBizStatus.activeStatuses());
        if (mongoTemplate.count(query, GroupMember.class) >= limitPerUser) {
            throw new ApiException(GROUP_RECORD_FAILED_HAVE_JOINED);
        }
    }

    /**
     * 预占库存。
     */
    private void reserveActivityStock(String activityId, int amount) {
        String stockKey = groupRedisKeyBuilder.buildActivityStockKey(activityId);
        ensureActivityStockCache(activityId, stockKey);
        Long remain = stringRedisTemplate.opsForValue().decrement(stockKey, amount);
        if (remain == null || remain < 0) {
            stringRedisTemplate.opsForValue().increment(stockKey, amount);
            throw new ApiException(GROUP_RECORD_STOCK_NOT_ENOUGH);
        }
        mongoTemplate.updateFirst(GroupActivity.buildIdQuery(activityId), new Update().inc("soldCount", amount), GroupActivity.class);
    }

    /**
     * 归还库存。
     */
    private void releaseActivityStock(String activityId, int amount) {
        if (amount <= 0) {
            return;
        }
        String stockKey = groupRedisKeyBuilder.buildActivityStockKey(activityId);
        ensureActivityStockCache(activityId, stockKey);
        stringRedisTemplate.opsForValue().increment(stockKey, amount);
        mongoTemplate.updateFirst(GroupActivity.buildIdQuery(activityId), new Update().inc("soldCount", -amount), GroupActivity.class);
    }

    /**
     * 初始化库存缓存。
     */
    private void ensureActivityStockCache(String activityId, String stockKey) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey))) {
            return;
        }
        GroupActivity activity = loadAndValidateActivity(activityId);
        int remain = Math.max(0, (activity.getTotalStock() == null ? 0 : activity.getTotalStock())
                - (activity.getSoldCount() == null ? 0 : activity.getSoldCount()));
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(remain));
    }

    /**
     * 发布成团消息。
     */
    private void publishGroupSuccess(String traceId, GroupInstance instance, List<GroupMember> members) {
        GroupSuccessMessage message = new GroupSuccessMessage();
        message.setTraceId(traceId);
        message.setGroupId(instance.getId());
        message.setActivityId(instance.getActivityId());
        message.setCompleteTime(instance.getCompleteTime());
        List<GroupSuccessMessage.MemberOrder> memberOrders = members.stream()
                .filter(item -> GroupMemberBizStatus.SUCCESS.name().equals(item.getMemberStatus()))
                .map(item -> {
                    GroupSuccessMessage.MemberOrder order = new GroupSuccessMessage.MemberOrder();
                    order.setUserId(item.getUserId());
                    order.setOrderId(item.getOrderId());
                    order.setIsLeader(item.getIsLeader() != null && item.getIsLeader() == 1);
                    return order;
                }).collect(Collectors.toList());
        message.setMemberOrders(memberOrders);
        rabbitMqPublisher.sendMsg(GroupMqConstant.GROUP_EXCHANGE, GroupMqConstant.GROUP_SUCCESS_KEY, message);
    }

    /**
     * 发布失败消息。
     */
    private void publishGroupFailed(String traceId, GroupInstance instance, String reason) {
        GroupFailedMessage message = new GroupFailedMessage();
        message.setTraceId(traceId);
        message.setGroupId(instance.getId());
        message.setActivityId(instance.getActivityId());
        message.setFailedTime(instance.getFailedTime());
        message.setReason(reason);
        rabbitMqPublisher.sendMsg(GroupMqConstant.GROUP_EXCHANGE, GroupMqConstant.GROUP_FAILED_KEY, message);
    }

    /**
     * 发布退款消息。
     */
    private void publishRefund(String traceId, GroupInstance instance, String reason,
                               List<GroupRefundMessage.RefundOrder> refundOrders) {
        GroupRefundMessage message = new GroupRefundMessage();
        message.setTraceId(traceId);
        message.setGroupId(instance.getId());
        message.setActivityId(instance.getActivityId());
        message.setReason(reason);
        message.setRefundOrders(refundOrders);
        rabbitMqPublisher.sendMsg(GroupMqConstant.GROUP_EXCHANGE, GroupMqConstant.GROUP_REFUND_KEY, message);
    }

    /**
     * 构建退款订单。
     */
    private GroupRefundMessage.RefundOrder buildRefundOrder(GroupMember member) {
        GroupRefundMessage.RefundOrder refundOrder = new GroupRefundMessage.RefundOrder();
        refundOrder.setUserId(member.getUserId());
        refundOrder.setOrderId(member.getOrderId());
        refundOrder.setRefundAmount(member.getGroupPrice());
        refundOrder.setIsLeader(member.getIsLeader() != null && member.getIsLeader() == 1);
        return refundOrder;
    }

    /**
     * 统计有效成员数。
     */
    private int countActiveMembers(List<GroupMember> members) {
        return (int) members.stream()
                .filter(item -> GroupMemberBizStatus.activeStatuses().contains(item.getMemberStatus()))
                .count();
    }

    /**
     * 构建成员摘要。
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
     * 转VO。
     */
    private GroupInstanceVO convertToVO(GroupInstance instance, List<GroupMember> members) {
        GroupInstanceVO vo = new GroupInstanceVO();
        vo.setId(instance.getId());
        vo.setActivityId(instance.getActivityId());
        vo.setLeaderUserId(instance.getLeaderUserId());
        vo.setStatus(instance.getStatus());
        vo.setRequiredSize(instance.getRequiredSize());
        vo.setCurrentSize(instance.getCurrentSize());
        vo.setRemainingSlots(instance.getRemainingSlots());
        vo.setExpireTime(instance.getExpireTime());
        vo.setCompleteTime(instance.getCompleteTime());
        vo.setGroupPrice(instance.getGroupPrice());
        vo.setSpuId(instance.getSpuId());
        vo.setSkuId(instance.getSkuId());
        vo.setSkuIds(instance.getSkuIds());
        vo.setFailReason(instance.getFailReason());
        List<GroupInstanceVO.MemberInfo> memberInfos = new ArrayList<>();
        for (GroupMember member : members) {
            GroupInstanceVO.MemberInfo memberInfo = new GroupInstanceVO.MemberInfo();
            memberInfo.setUserId(member.getUserId());
            memberInfo.setOrderId(member.getOrderId());
            memberInfo.setSkuId(member.getSkuId());
            memberInfo.setJoinTime(member.getJoinTime());
            memberInfo.setIsLeader(member.getIsLeader() != null && member.getIsLeader() == 1);
            memberInfo.setMemberStatus(member.getMemberStatus());
            memberInfo.setLatestTrajectory(member.getLatestTrajectory());
            memberInfo.setLatestTrajectoryTime(member.getLatestTrajectoryTime());
            memberInfos.add(memberInfo);
        }
        vo.setMembers(memberInfos);
        return vo;
    }

    /**
     * 追加轨迹。
     */
    private void appendTrajectory(GroupMember member, String code, String description, String source, String remark, Date eventTime) {
        List<GroupMember.TrajectoryNode> nodes = member.getTrajectories();
        if (nodes == null) {
            nodes = new ArrayList<>();
            member.setTrajectories(nodes);
        }
        GroupMember.TrajectoryNode node = new GroupMember.TrajectoryNode();
        node.setCode(code);
        node.setDescription(description);
        node.setSource(source);
        node.setRemark(remark);
        node.setEventTime(eventTime);
        nodes.add(node);
        if (nodes.size() > GroupBizConstants.MEMBER_TRAJECTORY_MAX_SIZE) {
            member.setTrajectories(new ArrayList<>(nodes.subList(
                    nodes.size() - GroupBizConstants.MEMBER_TRAJECTORY_MAX_SIZE, nodes.size())));
        }
        member.setLatestTrajectory(code);
        member.setLatestTrajectoryTime(eventTime);
    }

    /**
     * 执行锁内逻辑。
     */
    private <T> T executeUnderLock(String lockKey, Supplier<T> supplier) {
        RLock lock = redissonComponent.getRedissonClient().getLock(lockKey);
        try {
            boolean locked = lock.tryLock(GroupBizConstants.GROUP_LOCK_WAIT_SECONDS,
                    GroupBizConstants.GROUP_LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                throw new ApiException(GROUP_RECORD_ERROR.getMsg() + ": 获取业务锁失败");
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(GROUP_RECORD_ERROR.getMsg() + ": 锁等待被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 校验开团请求。
     */
    private void validateCreateCommand(CreateGroupCommand command) {
        if (command == null || !hasText(command.getActivityId()) || !hasText(command.getOrderId()) || command.getSkuId() == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
    }

    /**
     * 校验参团请求。
     */
    private void validateJoinCommand(JoinGroupCommand command) {
        if (command == null || !hasText(command.getGroupId()) || !hasText(command.getOrderId()) || command.getSkuId() == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
    }

    /**
     * 解析用户ID。
     */
    private Long resolveUserId(Long requestUserId) {
        try {
            if (AuthorizationContext.getClientUser() != null && AuthorizationContext.getClientUser().getId() != null) {
                return AuthorizationContext.getClientUser().getId();
            }
        } catch (Exception e) {
            log.debug("获取上下文用户失败", e);
        }
        if (requestUserId == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        return requestUserId;
    }

    /**
     * 必须存在的快照。
     */
    private LoadedGroupSnapshot requireSnapshot(String groupId) {
        LoadedGroupSnapshot snapshot = loadGroupSnapshot(groupId, true);
        if (snapshot == null) {
            throw new ApiException(GROUP_RECORD_NOT_EXISTS);
        }
        return snapshot;
    }

    /**
     * 构建Redis摘要。
     */
    private Map<String, String> buildSummaryMap(GroupInstance instance) {
        Map<String, String> summary = new LinkedHashMap<>();
        summary.put(SUMMARY_FIELD_ACTIVITY_ID, nullSafe(instance.getActivityId()));
        summary.put(SUMMARY_FIELD_LEADER_USER_ID, instance.getLeaderUserId() == null ? null : String.valueOf(instance.getLeaderUserId()));
        summary.put(SUMMARY_FIELD_STATUS, nullSafe(instance.getStatus()));
        summary.put(SUMMARY_FIELD_REQUIRED_SIZE, instance.getRequiredSize() == null ? null : String.valueOf(instance.getRequiredSize()));
        summary.put(SUMMARY_FIELD_CURRENT_SIZE, instance.getCurrentSize() == null ? null : String.valueOf(instance.getCurrentSize()));
        summary.put(SUMMARY_FIELD_REMAINING_SLOTS, instance.getRemainingSlots() == null ? null : String.valueOf(instance.getRemainingSlots()));
        summary.put(SUMMARY_FIELD_EXPIRE_TIME, instance.getExpireTime() == null ? null : String.valueOf(instance.getExpireTime().getTime()));
        summary.put(SUMMARY_FIELD_COMPLETE_TIME, instance.getCompleteTime() == null ? null : String.valueOf(instance.getCompleteTime().getTime()));
        summary.put(SUMMARY_FIELD_FAILED_TIME, instance.getFailedTime() == null ? null : String.valueOf(instance.getFailedTime().getTime()));
        summary.put(SUMMARY_FIELD_GROUP_PRICE, instance.getGroupPrice() == null ? null : instance.getGroupPrice().toPlainString());
        summary.put(SUMMARY_FIELD_SPU_ID, instance.getSpuId() == null ? null : String.valueOf(instance.getSpuId()));
        summary.put(SUMMARY_FIELD_SKU_IDS, toJson(instance.getSkuIds()));
        summary.put(SUMMARY_FIELD_FAIL_REASON, nullSafe(instance.getFailReason()));
        return summary;
    }

    /**
     * 序列化JSON。
     */
    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    /**
     * 比较日期。
     */
    private int compareDate(Date left, Date right) {
        Date leftValue = left != null ? left : new Date(0L);
        Date rightValue = right != null ? right : new Date(0L);
        return leftValue.compareTo(rightValue);
    }

    /**
     * Map取字符串。
     */
    private String getString(Map<Object, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Map取整型。
     */
    private Integer getInteger(Map<Object, Object> map, String key) {
        String value = getString(map, key);
        return !hasText(value) ? null : Integer.parseInt(value);
    }

    /**
     * Map取长整型。
     */
    private Long getLong(Map<Object, Object> map, String key) {
        String value = getString(map, key);
        return !hasText(value) ? null : Long.parseLong(value);
    }

    /**
     * Map取日期。
     */
    private Date getDate(Map<Object, Object> map, String key) {
        Long millis = getLong(map, key);
        return millis == null ? null : new Date(millis);
    }

    /**
     * Map取金额。
     */
    private BigDecimal getDecimal(Map<Object, Object> map, String key) {
        String value = getString(map, key);
        return !hasText(value) ? null : new BigDecimal(value);
    }

    /**
     * 判断字符串是否有值。
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 空值保护。
     */
    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    /**
     * 团快照。
     */
    private static class LoadedGroupSnapshot {
        private final GroupInstance instance;
        private final List<GroupMember> members;

        private LoadedGroupSnapshot(GroupInstance instance, List<GroupMember> members) {
            this.instance = instance;
            this.members = members != null ? members : new ArrayList<>();
        }
    }
}
