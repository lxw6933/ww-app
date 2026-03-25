package com.ww.mall.promotion.engine;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.app.common.exception.ApiException;
import com.ww.app.rabbitmq.RabbitMqPublisher;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.engine.model.GroupCacheSnapshot;
import com.ww.mall.promotion.engine.model.GroupCommandResult;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.enums.GroupEnabledStatus;
import com.ww.mall.promotion.enums.GroupTradeType;
import com.ww.mall.promotion.mq.GroupAfterSaleSuccessMessage;
import com.ww.mall.promotion.mq.GroupMqConstant;
import com.ww.mall.promotion.mq.GroupOrderPaidMessage;
import com.ww.mall.promotion.mq.GroupStateChangedMessage;
import com.ww.mall.promotion.service.group.command.CreateGroupCommand;
import com.ww.mall.promotion.service.group.command.JoinGroupCommand;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_ACTIVITY_EXPIRE_HOURS_INVALID;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_ACTIVITY_REQUIRED_SIZE_INVALID;
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
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_SKU_NOT_SUPPORTED;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_USER_FULL;

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
        resolveSkuRule(activity, command.getSkuId());
        String groupId = new ObjectId().toString();
        long nowMillis = System.currentTimeMillis();
        GroupCommandResult result = groupStorageComponent.createGroup(groupId, activity, command, nowMillis);
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
                : groupStorageComponent.findGroupIdByOrderId(message.getOrderId());
        if (!hasText(groupId)) {
            throw new ApiException(GROUP_RECORD_NOT_EXISTS);
        }
        GroupCacheSnapshot snapshot = groupStorageComponent.requireGroupSnapshot(groupId);
        String activityId = snapshot.getInstance().getActivityId();
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
                activityId,
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
        }
    }

    /**
     * 处理过期关团。
     *
     * @param groupId 团ID
     * @param reason 关团原因
     */
    public void expireGroup(String groupId, String reason) {
        GroupCacheSnapshot snapshot = groupStorageComponent.requireGroupSnapshot(groupId);
        long nowMillis = System.currentTimeMillis();
        int code = groupStorageComponent.expireGroup(groupId, snapshot.getInstance().getActivityId(), reason, nowMillis);
        if (code < 0 && code != -1 && code != -2) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        if (code == 1) {
            afterStateChanged(groupId, nowMillis);
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
     * Lua 成功后主链路不再同步执行 Mongo 投影，这里只做一次内部 MQ 投递。
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
}
