package com.ww.mall.promotion.service.group.impl;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.app.common.common.ResCode;
import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.common.exception.ApiException;
import com.ww.app.disruptor.api.DisruptorTemplate;
import com.ww.app.disruptor.model.Event;
import com.ww.app.rabbitmq.RabbitMqPublisher;
import com.ww.app.redis.component.lua.RedisScriptComponent;
import com.ww.mall.promotion.controller.app.group.req.CreateGroupRequest;
import com.ww.mall.promotion.controller.app.group.req.JoinGroupRequest;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.entity.group.GroupInstance;
import com.ww.mall.promotion.entity.group.GroupMember;
import com.ww.mall.promotion.enums.GroupMemberStatus;
import com.ww.mall.promotion.enums.GroupStatus;
import com.ww.mall.promotion.event.GroupEvent;
import com.ww.mall.promotion.key.GroupRedisKeyBuilder;
import com.ww.mall.promotion.mq.GroupMqConstant;
import com.ww.mall.promotion.mq.GroupRefundMessage;
import com.ww.mall.promotion.service.group.GroupInstanceService;
import com.ww.mall.promotion.service.group.convert.GroupConvert;
import com.ww.mall.promotion.service.group.support.GroupFlowLogSupport;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ww.mall.promotion.config.LuaScriptConfiguration.CREATE_GROUP_SCRIPT_NAME;
import static com.ww.mall.promotion.config.LuaScriptConfiguration.JOIN_GROUP_SCRIPT_NAME;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_CREATE_FAILED;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_ERROR;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_EXISTS;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_FAILED_DISABLE;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_FAILED_HAVE_JOINED;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_FAILED_TIME_END;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_FAILED_TIME_NOT_START;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_NOT_EXISTS;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_ORDER_CODE_NOT_EXISTS;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_ORDER_DUPLICATED;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_USER_FULL;

/**
 * 拼团实例服务实现。
 *
 * @author ww
 * @create 2025-12-08 17:45
 * @description: 拼团实例服务实现
 */
@Slf4j
@Service
public class GroupInstanceServiceImpl implements GroupInstanceService {

    private static final String SERVICE_SOURCE = "GROUP_SERVICE";

    @Resource
    private LoadingCache<String, GroupActivity> groupActivityCache;
    @Resource
    private RedisScriptComponent redisScriptComponent;
    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private GroupRedisKeyBuilder groupRedisKeyBuilder;
    @Resource
    private DisruptorTemplate<GroupEvent> groupDisruptorTemplate;
    @Resource
    private RabbitMqPublisher rabbitMqPublisher;
    @Resource
    private GroupFlowLogSupport groupFlowLogSupport;

    @Override
    public GroupInstanceVO createGroup(CreateGroupRequest request) {
        if (request == null || request.getActivityId() == null || request.getActivityId().trim().isEmpty()) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        if (request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
            throw new ApiException(GROUP_RECORD_ORDER_CODE_NOT_EXISTS);
        }
        GroupActivity activity = groupActivityCache.get(request.getActivityId());
        validateActivity(activity);
        Long userId = AuthorizationContext.getClientUser().getId();
        checkUserLimit(request.getActivityId(), userId, activity.getLimitPerUser());

        String traceId = groupFlowLogSupport.createTraceId();
        String groupId = new ObjectId().toString();
        groupFlowLogSupport.record(traceId, groupId, request.getActivityId(), userId, request.getOrderId(),
                "CREATE_GROUP", SERVICE_SOURCE, "PROCESSING", null, null, request);

        List<String> keys = Arrays.asList(
                groupRedisKeyBuilder.buildGroupMetaKey(groupId),
                groupRedisKeyBuilder.buildGroupSlotsKey(groupId),
                groupRedisKeyBuilder.buildGroupMembersKey(groupId),
                groupRedisKeyBuilder.buildGroupOrdersKey(groupId),
                groupRedisKeyBuilder.buildUserGroupKey(userId),
                groupRedisKeyBuilder.buildExpiryIndexKey()
        );
        long expireMillis = System.currentTimeMillis() + activity.getExpireHours() * 3600 * 1000L;
        List<String> args = Arrays.asList(
                GroupStatus.OPEN.getCode(),
                String.valueOf(activity.getRequiredSize()),
                String.valueOf(expireMillis),
                String.valueOf(userId),
                request.getOrderId(),
                request.getOrderInfo() != null ? request.getOrderInfo() : "{}",
                String.valueOf(activity.getRequiredSize() - 1),
                request.getActivityId(),
                groupId
        );

        try {
            Long result = redisScriptComponent.executeLuaScript(CREATE_GROUP_SCRIPT_NAME, ReturnType.INTEGER, keys, args);
            if (result == null || result != 1) {
                handleCreateGroupError(traceId, groupId, request.getActivityId(), result, userId,
                        request.getOrderId(), activity.getGroupPrice());
            }
            publishSaveInstanceEvent(traceId, groupId, request, activity, expireMillis, userId);
            groupFlowLogSupport.record(traceId, groupId, request.getActivityId(), userId, request.getOrderId(),
                    "CREATE_GROUP", SERVICE_SOURCE, "SUCCESS", null, null, request);
            return getGroupDetail(groupId);
        } catch (ApiException e) {
            groupFlowLogSupport.record(traceId, groupId, request.getActivityId(), userId, request.getOrderId(),
                    "CREATE_GROUP", SERVICE_SOURCE, "FAILED", null, e.getMessage(), request);
            throw e;
        } catch (Exception e) {
            log.error("创建拼团异常: groupId={}, traceId={}, userId={}, orderId={}",
                    groupId, traceId, userId, request.getOrderId(), e);
            sendRefundMessage(traceId, groupId, request.getActivityId(), userId,
                    request.getOrderId(), activity.getGroupPrice(), "创建拼团异常");
            throw new ApiException(GROUP_CREATE_FAILED.getMsg() + ": " + e.getMessage());
        }
    }

    @Override
    public GroupInstanceVO joinGroup(JoinGroupRequest request) {
        if (request == null || request.getGroupId() == null || request.getGroupId().trim().isEmpty()) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        if (request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
            throw new ApiException(GROUP_RECORD_ORDER_CODE_NOT_EXISTS);
        }
        String metaKey = groupRedisKeyBuilder.buildGroupMetaKey(request.getGroupId());
        Map<Object, Object> meta = stringRedisTemplate.opsForHash().entries(metaKey);
        if (meta.isEmpty()) {
            throw new ApiException(GROUP_RECORD_NOT_EXISTS);
        }
        String activityId = String.valueOf(meta.get("activityId"));
        GroupActivity activity = groupActivityCache.get(activityId);
        Long userId = AuthorizationContext.getClientUser().getId();
        checkUserLimit(activityId, userId, activity.getLimitPerUser());

        String traceId = groupFlowLogSupport.createTraceId();
        groupFlowLogSupport.record(traceId, request.getGroupId(), activityId, userId, request.getOrderId(),
                "JOIN_GROUP", SERVICE_SOURCE, "PROCESSING", null, null, request);

        List<String> keys = Arrays.asList(
                metaKey,
                groupRedisKeyBuilder.buildGroupSlotsKey(request.getGroupId()),
                groupRedisKeyBuilder.buildGroupMembersKey(request.getGroupId()),
                groupRedisKeyBuilder.buildGroupOrdersKey(request.getGroupId()),
                groupRedisKeyBuilder.buildExpiryIndexKey(),
                groupRedisKeyBuilder.buildUserGroupKey(userId)
        );
        List<String> args = Arrays.asList(
                String.valueOf(userId),
                request.getOrderId(),
                request.getOrderInfo() != null ? request.getOrderInfo() : "{}",
                request.getGroupId()
        );

        try {
            Object luaResult = redisScriptComponent.executeLuaScript(JOIN_GROUP_SCRIPT_NAME, ReturnType.MULTI, keys, args);
            if (!(luaResult instanceof List)) {
                throw new ApiException(GROUP_RECORD_ERROR);
            }
            List<?> resultList = (List<?>) luaResult;
            Long code = !resultList.isEmpty() && resultList.get(0) instanceof Long ? (Long) resultList.get(0) : null;
            Long newSlots = resultList.size() > 1 && resultList.get(1) instanceof Long ? (Long) resultList.get(1) : null;
            Long completeTime = resultList.size() > 2 && resultList.get(2) instanceof Long ? (Long) resultList.get(2) : null;
            if (code == null || code <= 0) {
                handleJoinGroupError(traceId, request.getGroupId(), activityId, code, userId,
                        request.getOrderId(), activity.getGroupPrice());
            }
            publishSaveMemberEvent(traceId, request.getGroupId(), request, activityId, newSlots, activity.getRequiredSize());
            if (code == 1) {
                publishGroupSuccessEvent(traceId, request.getGroupId(), activityId,
                        userId, request.getOrderId(), request.getOrderInfo(), completeTime);
            }
            groupFlowLogSupport.record(traceId, request.getGroupId(), activityId, userId, request.getOrderId(),
                    "JOIN_GROUP", SERVICE_SOURCE, "SUCCESS", null, null, request);
            return getGroupDetail(request.getGroupId());
        } catch (ApiException e) {
            groupFlowLogSupport.record(traceId, request.getGroupId(), activityId, userId, request.getOrderId(),
                    "JOIN_GROUP", SERVICE_SOURCE, "FAILED", null, e.getMessage(), request);
            throw e;
        } catch (Exception e) {
            log.error("加入拼团异常: groupId={}, traceId={}, userId={}, orderId={}",
                    request.getGroupId(), traceId, userId, request.getOrderId(), e);
            sendRefundMessage(traceId, request.getGroupId(), activityId, userId,
                    request.getOrderId(), activity.getGroupPrice(), "加入拼团异常");
            throw new ApiException(GROUP_RECORD_ERROR.getMsg() + ": " + e.getMessage());
        }
    }

    @Override
    public GroupInstanceVO getGroupDetail(String groupId) {
        if (groupId == null || groupId.trim().isEmpty()) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        Map<Object, Object> meta = stringRedisTemplate.opsForHash()
                .entries(groupRedisKeyBuilder.buildGroupMetaKey(groupId));
        if (meta.isEmpty()) {
            GroupInstance instance = mongoTemplate.findOne(GroupInstance.buildIdQuery(groupId), GroupInstance.class);
            if (instance == null) {
                throw new ApiException(GROUP_RECORD_NOT_EXISTS);
            }
            return convertToVO(instance);
        }

        GroupInstanceVO vo = new GroupInstanceVO();
        vo.setId(groupId);
        vo.setActivityId(getStringValue(meta.get("activityId"), null));
        vo.setLeaderUserId(getLongValue(meta.get("leaderUserId"), null));
        vo.setStatus(getStringValue(meta.get("status"), GroupStatus.OPEN.getCode()));
        vo.setRequiredSize(getIntegerValue(meta.get("requiredSize"), 1));
        vo.setCurrentSize(getIntegerValue(meta.get("currentSize"), 1));
        vo.setExpireTime(readDate(meta.get("expiresAt")));
        vo.setCompleteTime(readDate(meta.get("completeTime")));
        vo.setRemainingSlots(getIntegerValue(
                stringRedisTemplate.opsForValue().get(groupRedisKeyBuilder.buildGroupSlotsKey(groupId)), 0));

        GroupActivity activity = loadActivitySafely(vo.getActivityId());
        if (activity != null) {
            vo.setGroupPrice(activity.getGroupPrice());
            vo.setSpuId(activity.getSpuId());
            vo.setSkuId(activity.getSkuId());
        }

        Set<String> memberIds = stringRedisTemplate.opsForZSet()
                .range(groupRedisKeyBuilder.buildGroupMembersKey(groupId), 0, -1);
        if (memberIds == null || memberIds.isEmpty()) {
            return vo;
        }

        Map<Long, GroupMember> memberMap = mongoTemplate.find(GroupMember.buildGroupInstanceIdQuery(groupId), GroupMember.class)
                .stream().collect(Collectors.toMap(GroupMember::getUserId, member -> member, (left, right) -> left));
        List<GroupInstanceVO.MemberInfo> members = new ArrayList<>();
        for (String memberId : memberIds) {
            Long uid = getLongValue(memberId, null);
            if (uid == null) {
                continue;
            }
            GroupInstanceVO.MemberInfo memberInfo = new GroupInstanceVO.MemberInfo();
            memberInfo.setUserId(uid);
            memberInfo.setIsLeader(uid.equals(vo.getLeaderUserId()));
            GroupMember persisted = memberMap.get(uid);
            if (persisted != null) {
                memberInfo.setOrderId(persisted.getOrderId());
                memberInfo.setJoinTime(persisted.getJoinTime());
            } else {
                Double score = stringRedisTemplate.opsForZSet()
                        .score(groupRedisKeyBuilder.buildGroupMembersKey(groupId), memberId);
                if (score != null) {
                    memberInfo.setJoinTime(new Date(score.longValue()));
                }
            }
            members.add(memberInfo);
        }
        vo.setMembers(members);
        return vo;
    }

    @Override
    public List<GroupInstanceVO> getUserGroups() {
        Long userId = AuthorizationContext.getClientUser().getId();
        if (userId == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        Set<String> groupIds = stringRedisTemplate.opsForSet().members(groupRedisKeyBuilder.buildUserGroupKey(userId));
        if (groupIds == null || groupIds.isEmpty()) {
            return mongoTemplate.find(GroupMember.buildUserIdAndStatusQuery(userId, GroupMemberStatus.NORMAL.getCode()), GroupMember.class)
                    .stream().map(member -> getGroupDetail(member.getGroupInstanceId())).collect(Collectors.toList());
        }
        return groupIds.stream().map(this::getGroupDetail).collect(Collectors.toList());
    }

    @Override
    public List<GroupInstanceVO> getActivityGroups(String activityId, String status) {
        if (activityId == null || activityId.trim().isEmpty()) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        return mongoTemplate.find(GroupInstance.buildActivityIdAndStatusQuery(activityId, status), GroupInstance.class)
                .stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public void handleGroupSuccess(String groupId) {
        publishGroupSuccessEvent(groupFlowLogSupport.createTraceId(), groupId, null, null, null, null, null);
    }

    @Override
    public void handleGroupFailed(String groupId) {
        publishGroupFailedEvent(groupFlowLogSupport.createTraceId(), groupId, null, GROUP_RECORD_ERROR.getMsg());
    }

    /** 校验活动。 */
    private void validateActivity(GroupActivity activity) {
        Date now = new Date();
        if (activity == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        if (activity.getEnabled() == 0) {
            throw new ApiException(GROUP_RECORD_FAILED_DISABLE);
        }
        if (activity.getStartTime().after(now)) {
            throw new ApiException(GROUP_RECORD_FAILED_TIME_NOT_START);
        }
        if (activity.getEndTime().before(now)) {
            throw new ApiException(GROUP_RECORD_FAILED_TIME_END);
        }
    }

    /** 限购校验入口，当前保留 TODO。 */
    private void checkUserLimit(String activityId, Long userId, Integer limitPerUser) {
        if (limitPerUser != null && limitPerUser > 0) {
            log.debug("TODO 限购校验待接入订单查询: activityId={}, userId={}, limitPerUser={}",
                    activityId, userId, limitPerUser);
        }
    }

    /** 处理创建失败。 */
    private void handleCreateGroupError(String traceId, String groupId, String activityId, Long result,
                                        Long userId, String orderId, BigDecimal groupPrice) {
        ResCode errorCode = result != null && result == -1 ? GROUP_RECORD_FAILED_HAVE_JOINED
                : result != null && result == -2 ? GROUP_RECORD_ORDER_DUPLICATED
                : GROUP_CREATE_FAILED;
        boolean needRefund = result == null || result != -2;
        if (needRefund) {
            sendRefundMessage(traceId, groupId, activityId, userId, orderId, groupPrice, errorCode.getMsg());
        }
        throw new ApiException(errorCode);
    }

    /** 处理参团失败。 */
    private void handleJoinGroupError(String traceId, String groupId, String activityId, Long result,
                                      Long userId, String orderId, BigDecimal groupPrice) {
        ResCode errorCode;
        boolean needRefund = true;
        if (result == null || result == -1) {
            errorCode = GROUP_RECORD_NOT_EXISTS;
        } else if (result == -2) {
            errorCode = GROUP_RECORD_FAILED_TIME_NOT_START;
        } else if (result == -3) {
            errorCode = GROUP_RECORD_FAILED_TIME_END;
        } else if (result == -4) {
            errorCode = GROUP_RECORD_EXISTS;
        } else if (result == -5) {
            errorCode = GROUP_RECORD_ORDER_DUPLICATED;
            needRefund = false;
        } else if (result == -6) {
            errorCode = GROUP_RECORD_USER_FULL;
        } else {
            errorCode = GROUP_RECORD_ERROR;
        }
        if (needRefund) {
            sendRefundMessage(traceId, groupId, activityId, userId, orderId, groupPrice, errorCode.getMsg());
        }
        throw new ApiException(errorCode);
    }

    /** 发送退款消息。 */
    private void sendRefundMessage(String traceId, String groupId, String activityId, Long userId,
                                   String orderId, BigDecimal amount, String reason) {
        try {
            GroupRefundMessage.RefundOrder refundOrder = new GroupRefundMessage.RefundOrder();
            refundOrder.setUserId(userId);
            refundOrder.setOrderId(orderId);
            refundOrder.setRefundAmount(amount);
            refundOrder.setIsLeader(Boolean.FALSE);
            GroupRefundMessage message = new GroupRefundMessage();
            message.setTraceId(traceId);
            message.setGroupId(groupId);
            message.setActivityId(activityId);
            message.setReason(reason);
            message.setRefundOrders(Collections.singletonList(refundOrder));
            rabbitMqPublisher.sendMsg(GroupMqConstant.GROUP_EXCHANGE, GroupMqConstant.GROUP_REFUND_KEY, message);
            groupFlowLogSupport.record(traceId, groupId, activityId, userId, orderId,
                    "GROUP_REFUND_MQ", SERVICE_SOURCE, "SUCCESS", null, null, message);
        } catch (Exception e) {
            log.error("发送退款消息失败: traceId={}, groupId={}, orderId={}", traceId, groupId, orderId, e);
            groupFlowLogSupport.record(traceId, groupId, activityId, userId, orderId,
                    "GROUP_REFUND_MQ", SERVICE_SOURCE, "FAILED", null, e.getMessage(), reason);
        }
    }

    /** 发布实例保存事件。 */
    private void publishSaveInstanceEvent(String traceId, String groupId, CreateGroupRequest request,
                                          GroupActivity activity, long expireMillis, Long userId) {
        GroupEvent event = new GroupEvent(GroupEvent.EventType.SAVE_INSTANCE, groupId);
        event.setTraceId(traceId);
        event.setSource(SERVICE_SOURCE);
        event.setActivityId(request.getActivityId());
        event.setUserId(userId);
        event.setOrderId(request.getOrderId());
        event.setOrderInfo(request.getOrderInfo());
        event.addExtInfo("groupPrice", activity.getGroupPrice());
        event.addExtInfo("spuId", activity.getSpuId());
        event.addExtInfo("expireMillis", expireMillis);
        event.addExtInfo("requiredSize", activity.getRequiredSize());
        event.addExtInfo("currentSize", 1);
        event.addExtInfo("remainingSlots", activity.getRequiredSize() - 1);
        event.addExtInfo("status", GroupStatus.OPEN.getCode());
        event.addExtInfo("createdAt", System.currentTimeMillis());
        publishEvent("SAVE_INSTANCE", event);
    }

    /** 发布成员保存事件。 */
    private void publishSaveMemberEvent(String traceId, String groupId, JoinGroupRequest request,
                                        String activityId, Long newSlots, Integer requiredSize) {
        GroupEvent event = new GroupEvent(GroupEvent.EventType.SAVE_MEMBER, groupId);
        event.setTraceId(traceId);
        event.setSource(SERVICE_SOURCE);
        event.setActivityId(activityId);
        event.setUserId(AuthorizationContext.getClientUser().getId());
        event.setOrderId(request.getOrderId());
        event.setOrderInfo(request.getOrderInfo());
        GroupActivity activity = loadActivitySafely(activityId);
        if (activity != null) {
            event.addExtInfo("groupPrice", activity.getGroupPrice());
            event.addExtInfo("spuId", activity.getSpuId());
        }
        event.addExtInfo("joinTime", System.currentTimeMillis());
        if (newSlots != null) {
            event.addExtInfo("newSlots", newSlots);
            if (requiredSize != null) {
                event.addExtInfo("currentSize", requiredSize - newSlots.intValue());
            }
        }
        publishEvent("SAVE_MEMBER", event);
    }

    /** 发布拼团成功事件。 */
    private void publishGroupSuccessEvent(String traceId, String groupId, String activityId,
                                          Long userId, String orderId, String orderInfo, Long completeTime) {
        GroupEvent event = new GroupEvent(GroupEvent.EventType.GROUP_SUCCESS, groupId);
        event.setTraceId(traceId);
        event.setSource(SERVICE_SOURCE);
        event.setActivityId(activityId);
        event.setUserId(userId);
        event.setOrderId(orderId);
        event.setOrderInfo(orderInfo);
        if (completeTime != null) {
            event.addExtInfo("completeTime", completeTime);
        }
        publishEvent("GROUP_SUCCESS", event);
    }

    /** 发布拼团失败事件。 */
    private void publishGroupFailedEvent(String traceId, String groupId, String activityId, String reason) {
        GroupEvent event = new GroupEvent(GroupEvent.EventType.GROUP_FAILED, groupId);
        event.setTraceId(traceId);
        event.setSource(SERVICE_SOURCE);
        event.setActivityId(activityId);
        event.setErrorMessage(reason);
        event.addExtInfo("failedTime", System.currentTimeMillis());
        publishEvent("GROUP_FAILED", event);
    }

    /** 发布异步事件。 */
    private void publishEvent(String topic, GroupEvent event) {
        try {
            boolean published = groupDisruptorTemplate.publish(new Event<>(topic, event));
            if (!published) {
                groupFlowLogSupport.recordEvent(event, SERVICE_SOURCE, "FAILED", null, "发布事件失败:" + topic);
            }
        } catch (Exception e) {
            log.error("发布异步事件异常: topic={}, groupId={}, traceId={}", topic, event.getGroupId(), event.getTraceId(), e);
            groupFlowLogSupport.recordEvent(event, SERVICE_SOURCE, "FAILED", null, e.getMessage());
        }
    }

    /** 转 VO。 */
    private GroupInstanceVO convertToVO(GroupInstance instance) {
        return GroupConvert.INSTANCE.groupInstanceToVO(instance);
    }

    /** 安全取整型。 */
    private Integer getIntegerValue(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /** 安全取长整型。 */
    private Long getLongValue(Object value, Long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            String stringValue = String.valueOf(value).trim();
            return stringValue.isEmpty() || "null".equalsIgnoreCase(stringValue)
                    ? defaultValue : Long.parseLong(stringValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /** 安全取字符串。 */
    private String getStringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    /** 安全读日期。 */
    private Date readDate(Object value) {
        Long millis = getLongValue(value, null);
        return millis == null ? null : new Date(millis);
    }

    /** 安全加载活动。 */
    private GroupActivity loadActivitySafely(String activityId) {
        if (activityId == null || activityId.trim().isEmpty()) {
            return null;
        }
        try {
            return groupActivityCache.get(activityId);
        } catch (Exception e) {
            log.warn("加载活动缓存失败: activityId={}", activityId, e);
            return null;
        }
    }
}
