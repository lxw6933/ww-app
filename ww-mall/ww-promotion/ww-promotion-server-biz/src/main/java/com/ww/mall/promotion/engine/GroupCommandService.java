package com.ww.mall.promotion.engine;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.app.common.exception.ApiException;
import com.ww.app.rabbitmq.RabbitMqPublisher;
import com.ww.mall.promotion.component.GroupStorageComponent;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.engine.model.GroupCacheSnapshot;
import com.ww.mall.promotion.engine.model.GroupCommandResult;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.entity.group.GroupMember;
import com.ww.mall.promotion.enums.GroupMemberBizStatus;
import com.ww.mall.promotion.enums.GroupTradeType;
import com.ww.mall.promotion.mq.*;
import com.ww.mall.promotion.service.group.command.CreateGroupCommand;
import com.ww.mall.promotion.service.group.command.JoinGroupCommand;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static com.ww.mall.promotion.constants.ErrorCodeConstants.*;

/**
 * 拼团命令服务。
 * <p>
 * 该服务只负责业务编排、规则校验与异常语义映射。
 * 具体的 Redis Lua、Redis 快照、Mongo 投影与索引访问统一下沉到 {@link GroupStorageComponent}。
 *
 * @author ww
 * @create 2026-03-19
 * @description: 拼团命令服务
 */
@Slf4j
@Service
public class GroupCommandService {

    /**
     * 支付成功后，拼团域可直接判定为“应退款”的业务失败码集合。
     * <p>
     * 这些错误都表示“订单已支付，但未真正占到拼团席位”，
     * 下游应基于订单号发起退款补偿。
     */
    private static final Set<Integer> PAY_SUCCESS_AUTO_REFUND_ERROR_CODES = new HashSet<>(Arrays.asList(
            GROUP_RECORD_NOT_EXISTS.getCode(),
            GROUP_RECORD_EXISTS.getCode(),
            GROUP_RECORD_USER_FULL.getCode(),
            GROUP_RECORD_FAILED_TIME_NOT_START.getCode(),
            GROUP_RECORD_FAILED_TIME_END.getCode(),
            GROUP_RECORD_FAILED_DISABLE.getCode(),
            GROUP_RECORD_FAILED_CLOSED.getCode(),
            GROUP_RECORD_SKU_NOT_SUPPORTED.getCode()
    ));

    @Resource
    private LoadingCache<String, GroupActivity> groupActivityCache;

    @Resource
    private GroupStorageComponent groupStorageComponent;

    @Resource
    private GroupQueryService groupQueryService;

    @Resource
    private RabbitMqPublisher rabbitMqPublisher;

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
        ResolvedSkuRule resolvedSkuRule = resolveSkuRule(activity, command.getSkuId());
        String groupId = new ObjectId().toString();
        long nowMillis = System.currentTimeMillis();
        GroupCommandResult result = groupStorageComponent.createGroup(
                groupId,
                activity,
                command,
                nowMillis,
                resolvedSkuRule.getSpuId()
        );
        if (!result.isSuccess()) {
            throwCreateException(result);
        }
        afterStateChanged(result.getGroupId(), nowMillis);
        return groupQueryService.getGroupDetail(result.getGroupId());
    }

    /**
     * 参与拼团。
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
        GroupCacheSnapshot snapshot = groupStorageComponent.requireGroupSnapshot(command.getGroupId());
        GroupActivity activity = loadAndValidateActivity(snapshot.getInstance().getActivityId());
        resolveSkuRule(activity, command.getSkuId());
        long nowMillis = System.currentTimeMillis();
        GroupCommandResult result = groupStorageComponent.joinGroup(activity, command, nowMillis);
        if (!result.isSuccess()) {
            throwJoinException(result);
        }
        afterStateChanged(result.getGroupId(), nowMillis);
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
        try {
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
        } catch (ApiException e) {
            if (!shouldRequestRefundForPaidFailure(e)) {
                throw e;
            }
            requestRefundForPaidFailure(message, e.getMessage());
            log.warn("支付成功后拼团处理被业务规则拒绝，已发送退款补偿申请: orderId={}, tradeType={}, reason={}",
                    message.getOrderId(), message.getTradeType(), e.getMessage());
            return null;
        }
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
                : groupStorageComponent.findGroupIdByOrderId(message.getOrderId());
        if (!hasText(groupId)) {
            throw new ApiException(GROUP_RECORD_NOT_EXISTS);
        }
        GroupCacheSnapshot snapshot = groupStorageComponent.requireGroupSnapshot(groupId);
        Long userId = message.getUserId() != null ? message.getUserId()
                : groupStorageComponent.findMemberUserId(snapshot, message.getOrderId());
        if (userId == null) {
            throw new ApiException(GROUP_RECORD_ORDER_CODE_NOT_EXISTS);
        }
        long nowMillis = message.getSuccessTime() != null ? message.getSuccessTime().getTime() : System.currentTimeMillis();
        String failReason = groupStorageComponent.isLeader(snapshot, userId)
                ? "团长售后导致拼团关闭" : "售后成功，释放拼团名额";
        int code = groupStorageComponent.afterSaleSuccess(
                groupId,
                message.getAfterSaleId(),
                message.getOrderId(),
                nowMillis,
                failReason,
                message.getReason()
        );
        if (code < 0) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        if (code == 1) {
            afterStateChanged(groupId, nowMillis);
            if ("OPEN".equals(snapshot.getInstance().getStatus())) {
                requestRefundForPendingMembers(groupId, "GROUP_FAILED_REFUND", failReason,
                        new Date(nowMillis));
            }
        }
    }

    /**
     * 处理过期关团。
     *
     * @param groupId 团ID
     * @param reason 关团原因
     */
    public void expireGroup(String groupId, String reason) {
        long nowMillis = System.currentTimeMillis();
        groupStorageComponent.requireGroupSnapshot(groupId);
        int code = groupStorageComponent.expireGroup(groupId, reason, nowMillis);
        if (code < 0 && code != -1 && code != -2) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        if (code == 1) {
            afterStateChanged(groupId, nowMillis);
            requestRefundForPendingMembers(groupId, "GROUP_FAILED_REFUND", reason,
                    new Date(nowMillis));
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
        if (Boolean.FALSE.equals(activity.getEnabled())) {
            throw new ApiException(GROUP_RECORD_FAILED_DISABLE);
        }
        if (activity.getStartTime() != null && activity.getStartTime().after(now)) {
            throw new ApiException(GROUP_RECORD_FAILED_TIME_NOT_START);
        }
        if (activity.getEndTime() != null && !activity.getEndTime().after(now)) {
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
    private ResolvedSkuRule resolveSkuRule(GroupActivity activity, Long skuId) {
        if (activity == null || skuId == null) {
            throw new ApiException(GROUP_RECORD_SKU_NOT_SUPPORTED);
        }
        if (activity.getSpuConfigs() == null || activity.getSpuConfigs().isEmpty()) {
            throw new ApiException(GROUP_RECORD_SKU_NOT_SUPPORTED);
        }
        for (GroupActivity.GroupSpuConfig spuConfig : activity.getSpuConfigs()) {
            if (spuConfig == null || spuConfig.getSpuId() == null || spuConfig.getSkuRules() == null) {
                continue;
            }
            for (GroupActivity.GroupSkuRule rule : spuConfig.getSkuRules()) {
                if (rule == null || !skuId.equals(rule.getSkuId()) || Boolean.FALSE.equals(rule.getEnabled())) {
                    continue;
                }
                ResolvedSkuRule resolvedSkuRule = new ResolvedSkuRule();
                resolvedSkuRule.setSpuId(spuConfig.getSpuId());
                resolvedSkuRule.setSkuRule(rule);
                return resolvedSkuRule;
            }
        }
        throw new ApiException(GROUP_RECORD_SKU_NOT_SUPPORTED);
    }

    /**
     * 命中的 SKU 规则及其所属 SPU。
     */
    @lombok.Data
    private static class ResolvedSkuRule {

        /**
         * 命中的 SPU ID。
         */
        private Long spuId;

        /**
         * 命中的 SKU 规则。
         */
        private GroupActivity.GroupSkuRule skuRule;
    }

    /**
     * 抛出开团异常。
     *
     * @param result 命令结果
     */
    private void throwCreateException(GroupCommandResult result) {
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
            if ("FAILED".equals(result.getGroupStatus())) {
                throw new ApiException(GROUP_RECORD_FAILED_CLOSED);
            }
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        if ("-5".equals(result.getFailReason())) {
            throw new ApiException(GROUP_RECORD_FAILED_TIME_END);
        }
        if ("-6".equals(result.getFailReason())) {
            throw new ApiException(GROUP_RECORD_EXISTS);
        }
        if ("-8".equals(result.getFailReason())) {
            throw new ApiException(GROUP_RECORD_USER_FULL);
        }
        throw new ApiException(GROUP_RECORD_ERROR);
    }

    /**
     * 在正式开团前按订单进行幂等回放。
     * <p>
     * 仅依赖 Redis 订单索引做幂等判断，避免回放链路再依赖 Mongo 投影。
     *
     * @param command 开团命令
     * @return 已存在的团详情，不存在时返回 null
     */
    private GroupInstanceVO replayCreateGroup(CreateGroupCommand command) {
        String existingGroupId = findExistingGroupId(command.getOrderId());
        if (!hasText(existingGroupId)) {
            return null;
        }
        syncProjection(existingGroupId);
        return groupQueryService.getGroupDetail(existingGroupId);
    }

    /**
     * 在正式参团前按订单进行幂等回放。
     *
     * @param command 参团命令
     * @return 已存在的团详情，不存在时返回 null
     */
    private GroupInstanceVO replayJoinGroup(JoinGroupCommand command) {
        String existingGroupId = findExistingGroupId(command.getOrderId());
        if (!hasText(existingGroupId)) {
            return null;
        }
        if (!existingGroupId.equals(command.getGroupId())) {
            throw new ApiException(GROUP_RECORD_ORDER_DUPLICATED);
        }
        syncProjection(existingGroupId);
        return groupQueryService.getGroupDetail(existingGroupId);
    }

    /**
     * 尝试投递内部状态变更 MQ。
     * <p>
     * Lua 成功后主链路优先投递一次内部 MQ，由消费者异步把 Redis 快照落到 Mongo。
     * 若 MQ 投递失败，则退化为当前线程直接同步一次 Mongo 投影，降低查询模型长时间不一致的概率。
     *
     * @param groupId 团ID
     * @param eventTimeMillis 状态变更发生时间毫秒值
     */
    private void afterStateChanged(String groupId, long eventTimeMillis) {
        if (!hasText(groupId) || eventTimeMillis <= 0L) {
            return;
        }
        GroupStateChangedMessage message = new GroupStateChangedMessage();
        message.setGroupId(groupId);
        message.setEventTime(new Date(eventTimeMillis));
        try {
            rabbitMqPublisher.sendMsg(
                    GroupMqConstant.GROUP_EXCHANGE,
                    GroupMqConstant.GROUP_STATE_CHANGED_KEY,
                    message
            );
        } catch (Exception e) {
            log.error("拼团状态变更内部消息发送失败: groupId={}, eventTimeMillis={}",
                    message.getGroupId(), eventTimeMillis, e);
            try {
                syncProjection(groupId);
                log.warn("拼团状态变更内部消息发送失败，已执行本地Mongo投影兜底: groupId={}", groupId);
            } catch (Exception projectionException) {
                log.error("拼团状态变更内部消息发送失败后，本地Mongo投影兜底仍失败: groupId={}",
                        groupId, projectionException);
            }
        }
    }

    /**
     * 同步 Mongo 投影。
     *
     * @param groupId 团ID
     * @return 最新快照
     */
    private GroupCacheSnapshot syncProjection(String groupId) {
        if (!hasText(groupId)) {
            return null;
        }
        return groupStorageComponent.syncProjection(groupId);
    }

    /**
     * 查询订单已归属的拼团ID。
     *
     * @param orderId 订单ID
     * @return 拼团ID
     */
    private String findExistingGroupId(String orderId) {
        return groupStorageComponent.findGroupIdByOrderId(orderId);
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
     * 判断文本是否有值。
     *
     * @param value 文本
     * @return true-有值
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 判断支付成功后的业务失败是否应触发退款补偿。
     * <p>
     * 这里只接收“已支付但未占位成功”的确定性失败；
     * 对于系统异常、状态未知、消息重复等场景，不在本方法内直接退款，
     * 避免误退或重复退。
     *
     * @param exception 业务异常
     * @return true-应触发退款补偿
     */
    private boolean shouldRequestRefundForPaidFailure(ApiException exception) {
        return exception != null
                && exception.getCode() != null
                && PAY_SUCCESS_AUTO_REFUND_ERROR_CODES.contains(exception.getCode());
    }

    /**
     * 针对“支付成功但未成功入团”的场景发送退款补偿申请。
     *
     * @param message 支付成功消息
     * @param reason 失败原因
     */
    private void requestRefundForPaidFailure(GroupOrderPaidMessage message, String reason) {
        GroupRefundRequestMessage refundMessage = new GroupRefundRequestMessage();
        refundMessage.setGroupId(message.getGroupId());
        refundMessage.setActivityId(message.getActivityId());
        refundMessage.setUserId(message.getUserId());
        refundMessage.setOrderId(message.getOrderId());
        refundMessage.setRefundAmount(message.getPayAmount());
        refundMessage.setRefundScene(message.getTradeType() == GroupTradeType.START
                ? "GROUP_CREATE_REJECTED" : "GROUP_JOIN_REJECTED");
        refundMessage.setReason(reason);
        refundMessage.setEventTime(new Date());
        publishRefundRequest(refundMessage);
    }

    /**
     * 为当前团中处于“待退款”状态的成员批量发送退款补偿申请。
     * <p>
     * 该方法只读取已经落入 Redis 终态快照中的成员状态，
     * 仅对 {@link GroupMemberBizStatus#FAILED_REFUND_PENDING} 成员发消息，
     * 从而保证普通售后释放名额等非退款场景不会被误触发。
     *
     * @param groupId 团ID
     * @param refundScene 退款场景
     * @param reason 失败原因
     * @param eventTime 事件时间
     */
    private void requestRefundForPendingMembers(String groupId, String refundScene, String reason, Date eventTime) {
        if (!hasText(groupId)) {
            return;
        }
        GroupCacheSnapshot snapshot = groupStorageComponent.requireGroupSnapshot(groupId);
        if (snapshot == null || snapshot.getInstance() == null || snapshot.getMembers() == null) {
            return;
        }
        snapshot.getMembers().forEach(member ->
                requestRefundForPendingMember(snapshot, member, refundScene, reason, eventTime));
    }

    /**
     * 为单个待退款成员发送退款补偿申请。
     * <p>
     * 只有订单号和退款金额完整时才发送消息；
     * 若关键数据缺失，则保留成员的待退款状态并记录错误日志，留待人工或后续任务补偿。
     *
     * @param snapshot 团快照
     * @param member 成员快照
     * @param refundScene 退款场景
     * @param reason 退款原因
     * @param eventTime 事件时间
     */
    private void requestRefundForPendingMember(GroupCacheSnapshot snapshot, GroupMember member,
                                               String refundScene, String reason, Date eventTime) {
        if (member == null
                || !GroupMemberBizStatus.FAILED_REFUND_PENDING.name().equals(member.getMemberStatus())) {
            return;
        }
        if (!hasText(member.getOrderId()) || member.getPayAmount() == null) {
            log.error("拼团退款补偿消息未发送，成员关键数据缺失: groupId={}, orderId={}, memberStatus={}",
                    snapshot.getInstance().getId(), member.getOrderId(), member.getMemberStatus());
            return;
        }
        GroupRefundRequestMessage refundMessage = new GroupRefundRequestMessage();
        refundMessage.setGroupId(snapshot.getInstance().getId());
        refundMessage.setActivityId(snapshot.getInstance().getActivityId());
        refundMessage.setUserId(member.getUserId());
        refundMessage.setOrderId(member.getOrderId());
        refundMessage.setRefundAmount(member.getPayAmount());
        refundMessage.setRefundScene(refundScene);
        refundMessage.setReason(hasText(snapshot.getInstance().getFailReason())
                ? snapshot.getInstance().getFailReason() : reason);
        refundMessage.setEventTime(eventTime != null ? eventTime : new Date());
        try {
            publishRefundRequest(refundMessage);
        } catch (RuntimeException e) {
            log.error("拼团退款补偿消息发送失败: groupId={}, orderId={}, refundScene={}",
                    refundMessage.getGroupId(), refundMessage.getOrderId(), refundScene, e);
        }
    }

    /**
     * 发送退款补偿申请消息。
     *
     * @param message 退款补偿消息
     */
    private void publishRefundRequest(GroupRefundRequestMessage message) {
        rabbitMqPublisher.sendMsg(
                GroupMqConstant.GROUP_EXCHANGE,
                GroupMqConstant.GROUP_REFUND_REQUEST_KEY,
                message
        );
    }
}
