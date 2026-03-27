package com.ww.mall.promotion.engine;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.app.common.exception.ApiException;
import com.ww.app.rabbitmq.RabbitMqPublisher;
import com.ww.mall.promotion.component.GroupStorageComponent;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.dto.group.GroupAfterSaleRequestDTO;
import com.ww.mall.promotion.engine.model.GroupCacheSnapshot;
import com.ww.mall.promotion.engine.model.GroupCommandResult;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.entity.group.GroupMember;
import com.ww.mall.promotion.enums.GroupAfterSaleScene;
import com.ww.mall.promotion.enums.GroupCompensationTaskType;
import com.ww.mall.promotion.enums.GroupMemberBizStatus;
import com.ww.mall.promotion.enums.GroupStatus;
import com.ww.mall.promotion.enums.GroupTradeType;
import com.ww.mall.promotion.mq.*;
import com.ww.mall.promotion.service.group.command.CreateGroupCommand;
import com.ww.mall.promotion.service.group.command.JoinGroupCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
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
     * OPEN 拼团申请售后时发往下游的退款场景码。
     */
    private static final String GROUP_OPEN_AFTER_SALE_REFUND_SCENE = "GROUP_OPEN_AFTER_SALE_REFUND";

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
        String groupId = command.getGroupId();
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
        if (result.isReplayed()) {
            syncProjection(result.getGroupId());
            return groupQueryService.getGroupDetail(result.getGroupId());
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
        ResolvedSkuRule resolvedSkuRule = resolveSkuRule(activity, command.getSkuId());
        validateJoinSpuConsistency(snapshot, resolvedSkuRule);
        long nowMillis = System.currentTimeMillis();
        GroupCommandResult result = groupStorageComponent.joinGroup(activity, command, nowMillis);
        if (!result.isSuccess()) {
            throwJoinException(result);
        }
        if (result.isReplayed()) {
            syncProjection(result.getGroupId());
            return groupQueryService.getGroupDetail(result.getGroupId());
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
                command.setGroupId(message.getGroupId());
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
     * 处理拼团售后。
     *
     * @param request 售后请求
     */
    public void handleAfterSale(GroupAfterSaleRequestDTO request) {
        validateAfterSaleRequest(request);
        if (request.getScene() == GroupAfterSaleScene.TRADE_EXCEPTION_REFUND) {
            requestRefundForTradeException(request);
            return;
        }
        handleOpenAfterSale(request);
    }

    /**
     * 处理 OPEN 拼团中的售后申请。
     * <p>
     * 该场景下订单已经真实占用了拼团席位，因此需要先执行拼团售后脚本，
     * 再发送当前售后订单的退款消息；若是团长售后导致整团关闭，则还要继续补发其他成员退款消息。
     *
     * @param request 售后请求
     */
    private void handleOpenAfterSale(GroupAfterSaleRequestDTO request) {
        if (!hasText(request.getGroupId())) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        GroupCacheSnapshot snapshot = groupStorageComponent.loadGroupSnapshot(request.getGroupId());
        if (snapshot == null || snapshot.getInstance() == null) {
            log.info("OPEN 拼团售后请求未读取到拼团快照，按直接退款处理: groupId={}, orderId={}",
                    request.getGroupId(), request.getOrderId());
            requestRefundForAfterSaleWithoutScript(request, null, null);
            return;
        }
        if (!GroupStatus.OPEN.getCode().equals(snapshot.getInstance().getStatus())) {
            log.info("拼团售后请求对应拼团非 OPEN 状态，跳过拼团售后脚本: groupId={}, orderId={}, groupStatus={}",
                    request.getGroupId(), request.getOrderId(), snapshot.getInstance().getStatus());
            return;
        }
        GroupMember targetMember = findMember(snapshot, request.getOrderId());
        if (targetMember == null) {
            log.info("OPEN 拼团售后请求未命中成员记录，按直接退款处理: groupId={}, orderId={}",
                    request.getGroupId(), request.getOrderId());
            requestRefundForAfterSaleWithoutScript(request, snapshot, null);
            return;
        }
        Long targetUserId = request.getUserId() != null ? request.getUserId() : targetMember.getUserId();
        long nowMillis = request.getEventTime() != null ? request.getEventTime().getTime() : System.currentTimeMillis();
        Date eventTime = new Date(nowMillis);
        boolean leaderAfterSale = isLeader(snapshot, targetUserId);
        String failReason = leaderAfterSale
                ? "LEADER_AFTER_SALE_CLOSE_GROUP"
                : "OPEN_AFTER_SALE_RELEASE_SLOT";
        int code = groupStorageComponent.afterSaleSuccess(
                request.getGroupId(),
                request.getAfterSaleId(),
                request.getOrderId(),
                nowMillis,
                failReason,
                request.getReason()
        );
        if (code < 0) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        if (code == 3) {
            log.info("拼团售后脚本命中并发 no-op，当前拼团已非 OPEN，跳过退款直发: groupId={}, orderId={}",
                    request.getGroupId(), request.getOrderId());
            return;
        }
        if (code == 1) {
            afterStateChanged(request.getGroupId(), nowMillis);
            if (leaderAfterSale) {
                requestRefundForPendingMembers(request.getGroupId(), "GROUP_FAILED_REFUND", failReason, eventTime);
            }
        }
        requestRefundForOpenAfterSale(request, snapshot, targetMember, eventTime);
    }

    /**
     * 处理“支付后创建团/参团异常”的直接退款。
     *
     * @param request 售后请求
     */
    private void requestRefundForTradeException(GroupAfterSaleRequestDTO request) {
        GroupRefundRequestMessage refundMessage = new GroupRefundRequestMessage();
        refundMessage.setGroupId(request.getGroupId());
        refundMessage.setActivityId(request.getActivityId());
        refundMessage.setUserId(request.getUserId());
        refundMessage.setOrderId(request.getOrderId());
        refundMessage.setRefundAmount(resolveRefundAmount(request, null));
        refundMessage.setRefundScene(resolveTradeExceptionRefundScene(request.getTradeType()));
        refundMessage.setReason(request.getReason());
        refundMessage.setEventTime(request.getEventTime() != null ? request.getEventTime() : new Date());
        publishRefundRequest(refundMessage);
    }

    /**
     * 当 OPEN 售后未命中有效拼团成员时，按“无需执行拼团脚本、直接退款”兜底处理。
     * <p>
     * 该场景主要兼容“支付后创建团/参团异常”或订单侧重试时拼团数据已经不存在的情况。
     *
     * @param request 售后请求
     * @param snapshot 拼团快照
     * @param targetMember 命中的成员
     */
    private void requestRefundForAfterSaleWithoutScript(GroupAfterSaleRequestDTO request,
                                                        GroupCacheSnapshot snapshot,
                                                        GroupMember targetMember) {
        GroupRefundRequestMessage refundMessage = new GroupRefundRequestMessage();
        refundMessage.setGroupId(request.getGroupId());
        refundMessage.setActivityId(resolveActivityId(snapshot, request));
        refundMessage.setUserId(request.getUserId() != null ? request.getUserId()
                : (targetMember == null ? null : targetMember.getUserId()));
        refundMessage.setOrderId(request.getOrderId());
        refundMessage.setRefundAmount(resolveRefundAmount(request, targetMember));
        refundMessage.setRefundScene(resolveAfterSaleRefundScene(request));
        refundMessage.setReason(request.getReason());
        refundMessage.setEventTime(request.getEventTime() != null ? request.getEventTime() : new Date());
        publishRefundRequest(refundMessage);
    }

    /**
     * 为 OPEN 拼团中的当前售后订单发送退款消息。
     *
     * @param request 售后请求
     * @param snapshot 拼团快照
     * @param targetMember 命中的成员
     * @param eventTime 事件时间
     */
    private void requestRefundForOpenAfterSale(GroupAfterSaleRequestDTO request, GroupCacheSnapshot snapshot,
                                               GroupMember targetMember, Date eventTime) {
        GroupRefundRequestMessage refundMessage = new GroupRefundRequestMessage();
        refundMessage.setGroupId(request.getGroupId());
        refundMessage.setActivityId(resolveActivityId(snapshot, request));
        refundMessage.setUserId(request.getUserId() != null ? request.getUserId() : targetMember.getUserId());
        refundMessage.setOrderId(request.getOrderId());
        refundMessage.setRefundAmount(resolveRefundAmount(request, targetMember));
        refundMessage.setRefundScene(GROUP_OPEN_AFTER_SALE_REFUND_SCENE);
        refundMessage.setReason(request.getReason());
        refundMessage.setEventTime(eventTime != null ? eventTime : new Date());
        publishRefundRequest(refundMessage);
    }

    /**
     * 校验拼团售后请求。
     *
     * @param request 售后请求
     */
    private void validateAfterSaleRequest(GroupAfterSaleRequestDTO request) {
        if (request == null || request.getScene() == null || !hasText(request.getOrderId())) {
            throw new ApiException(GROUP_RECORD_ORDER_CODE_NOT_EXISTS);
        }
        if (request.getScene() == GroupAfterSaleScene.OPEN_APPLY && !hasText(request.getGroupId())) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        if (request.getScene() == GroupAfterSaleScene.TRADE_EXCEPTION_REFUND && request.getTradeType() == null) {
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
     * 判断指定拼团当前是否仍存在待退款成员。
     * <p>
     * 该方法只依据拼团快照中的成员状态判断，
     * 适合用于人工补偿或任务补偿前的快速探测。
     *
     * @param groupId 团ID
     * @return true-仍有待退款成员，false-当前无需补偿
     */
    public boolean hasPendingRefund(String groupId) {
        GroupCacheSnapshot snapshot = groupStorageComponent.requireGroupSnapshot(groupId);
        return hasPendingRefundMembers(snapshot);
    }

    /**
     * 触发指定失败拼团的待退款成员补偿重发。
     * <p>
     * 下游应继续基于订单号和退款场景做幂等，
     * 因此该方法允许人工重复触发，作为 MQ 失败后的补偿入口。
     *
     * @param groupId 团ID
     * @param reason 重发原因
     * @return 本次成功投递的退款补偿消息数
     */
    public int triggerPendingRefundCompensation(String groupId, String reason) {
        GroupCacheSnapshot snapshot = groupStorageComponent.requireGroupSnapshot(groupId);
        if (snapshot.getInstance() == null
                || !GroupStatus.FAILED.getCode().equals(snapshot.getInstance().getStatus())) {
            log.info("当前拼团非FAILED终态，跳过待退款补偿重发: groupId={}, status={}",
                    groupId, snapshot.getInstance() == null ? null : snapshot.getInstance().getStatus());
            return 0;
        }
        if (!hasPendingRefundMembers(snapshot)) {
            log.info("当前拼团不存在待退款成员，跳过待退款补偿重发: groupId={}", groupId);
            return 0;
        }
        String finalReason = hasText(reason) ? reason
                : (hasText(snapshot.getInstance().getFailReason())
                ? snapshot.getInstance().getFailReason()
                : "人工触发拼团退款补偿");
        return requestRefundForPendingMembers(snapshot, "GROUP_FAILED_REFUND", finalReason, new Date());
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
     * 校验参团 SKU 与当前拼团绑定的 SPU 是否一致。
     * <p>
     * 开团成功后，团主状态已经固化了本团命中的 SPU ID，
     * 后续参团必须继续命中同一个 SPU，避免同一个团下混入不同商品。
     *
     * @param snapshot 当前拼团快照
     * @param resolvedSkuRule 本次参团命中的 SKU 规则
     */
    private void validateJoinSpuConsistency(GroupCacheSnapshot snapshot, ResolvedSkuRule resolvedSkuRule) {
        if (snapshot == null || snapshot.getInstance() == null || resolvedSkuRule == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        Long groupSpuId = snapshot.getInstance().getSpuId();
        Long requestSpuId = resolvedSkuRule.getSpuId();
        if (groupSpuId == null || requestSpuId == null) {
            log.warn("参团SPU一致性校验失败，团快照或请求SPU缺失: groupId={}, groupSpuId={}, requestSpuId={}",
                    snapshot.getInstance().getId(), groupSpuId, requestSpuId);
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        if (!groupSpuId.equals(requestSpuId)) {
            log.warn("参团命中跨SPU混团校验，拒绝参团: groupId={}, groupSpuId={}, requestSpuId={}, skuId={}",
                    snapshot.getInstance().getId(), groupSpuId, requestSpuId,
                    resolvedSkuRule.getSkuRule() == null ? null : resolvedSkuRule.getSkuRule().getSkuId());
            throw new ApiException(GROUP_RECORD_SPU_NOT_MATCH);
        }
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
     * 在正式开团前按 groupId 进行幂等回放。
     * <p>
     * 上游在下单阶段已经生成 groupId，因此重试开团时只需按 groupId 检查
     * 当前拼团快照是否已存在，无需再维护订单到拼团的反向索引。
     *
     * @param command 开团命令
     * @return 已存在的团详情，不存在时返回 null
     */
    private GroupInstanceVO replayCreateGroup(CreateGroupCommand command) {
        GroupCacheSnapshot snapshot = groupStorageComponent.loadGroupSnapshot(command.getGroupId());
        if (snapshot == null || snapshot.getInstance() == null) {
            return null;
        }
        syncProjection(command.getGroupId());
        return groupQueryService.getGroupDetail(command.getGroupId());
    }

    /**
     * 在正式参团前按 groupId + 团内 orderId 进行幂等回放。
     * <p>
     * 参团场景由上游保证 groupId 与订单绑定关系正确，
     * 拼团域只需确认该订单是否已经写入当前团的成员仓库。
     *
     * @param command 参团命令
     * @return 已存在的团详情，不存在时返回 null
     */
    private GroupInstanceVO replayJoinGroup(JoinGroupCommand command) {
        GroupCacheSnapshot snapshot = groupStorageComponent.loadGroupSnapshot(command.getGroupId());
        if (snapshot == null || snapshot.getInstance() == null) {
            return null;
        }
        if (!groupStorageComponent.existsMemberOrder(command.getGroupId(), command.getOrderId())) {
            return null;
        }
        syncProjection(command.getGroupId());
        return groupQueryService.getGroupDetail(command.getGroupId());
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
                groupStorageComponent.submitCompensationTask(
                        GroupCompensationTaskType.PROJECTION_SYNC,
                        groupId,
                        new Date(eventTimeMillis),
                        projectionException.getMessage()
                );
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
     * 校验支付消息。
     *
     * @param message 支付消息
     */
    private void validatePaidMessage(GroupOrderPaidMessage message) {
        if (message == null || message.getTradeType() == null || !hasText(message.getGroupId())
                || !hasText(message.getOrderId())
                || message.getUserId() == null || message.getSkuId() == null || message.getPayAmount() == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        if (message.getTradeType() == GroupTradeType.START && !hasText(message.getActivityId())) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
    }

    /**
     * 校验开团命令。
     *
     * @param command 开团命令
     */
    private void validateCreateCommand(CreateGroupCommand command) {
        if (command == null || !hasText(command.getGroupId()) || !hasText(command.getActivityId())
                || !hasText(command.getOrderId())
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
     * 根据订单号在拼团快照中查找成员。
     *
     * @param snapshot 拼团快照
     * @param orderId 订单号
     * @return 命中的成员；未命中返回 {@code null}
     */
    private GroupMember findMember(GroupCacheSnapshot snapshot, String orderId) {
        if (snapshot == null || snapshot.getMembers() == null || !hasText(orderId)) {
            return null;
        }
        return snapshot.getMembers().stream()
                .filter(Objects::nonNull)
                .filter(member -> orderId.equals(member.getOrderId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断指定用户是否为当前拼团团长。
     *
     * @param snapshot 拼团快照
     * @param userId 用户ID
     * @return true-团长
     */
    private boolean isLeader(GroupCacheSnapshot snapshot, Long userId) {
        return snapshot != null
                && snapshot.getInstance() != null
                && snapshot.getInstance().getLeaderUserId() != null
                && snapshot.getInstance().getLeaderUserId().equals(userId);
    }

    /**
     * 解析退款金额。
     * <p>
     * 优先使用订单域显式透传的金额；若未透传，则回退读取拼团成员快照中的实付金额。
     *
     * @param request 售后请求
     * @param targetMember 命中的成员
     * @return 退款金额
     */
    private java.math.BigDecimal resolveRefundAmount(GroupAfterSaleRequestDTO request, GroupMember targetMember) {
        java.math.BigDecimal refundAmount = request.getRefundAmount() != null
                ? request.getRefundAmount()
                : (targetMember == null ? null : targetMember.getPayAmount());
        if (refundAmount == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        return refundAmount;
    }

    /**
     * 解析活动ID。
     *
     * @param snapshot 拼团快照
     * @param request 售后请求
     * @return 活动ID
     */
    private String resolveActivityId(GroupCacheSnapshot snapshot, GroupAfterSaleRequestDTO request) {
        if (hasText(request.getActivityId())) {
            return request.getActivityId();
        }
        return snapshot != null && snapshot.getInstance() != null ? snapshot.getInstance().getActivityId() : null;
    }

    /**
     * 解析售后兜底退款场景码。
     *
     * @param request 售后请求
     * @return 退款场景码
     */
    private String resolveAfterSaleRefundScene(GroupAfterSaleRequestDTO request) {
        if (request.getTradeType() == null) {
            return GROUP_OPEN_AFTER_SALE_REFUND_SCENE;
        }
        return resolveTradeExceptionRefundScene(request.getTradeType());
    }

    /**
     * 根据创建团/参团类型解析退款场景码。
     *
     * @param tradeType 交易类型
     * @return 退款场景码
     */
    private String resolveTradeExceptionRefundScene(GroupTradeType tradeType) {
        return tradeType == GroupTradeType.START ? "GROUP_CREATE_REJECTED" : "GROUP_JOIN_REJECTED";
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
    private int requestRefundForPendingMembers(String groupId, String refundScene, String reason, Date eventTime) {
        if (!hasText(groupId)) {
            return 0;
        }
        GroupCacheSnapshot snapshot = groupStorageComponent.requireGroupSnapshot(groupId);
        if (snapshot == null || snapshot.getInstance() == null || snapshot.getMembers() == null) {
            return 0;
        }
        return requestRefundForPendingMembers(snapshot, refundScene, reason, eventTime);
    }

    /**
     * 按快照批量发送待退款成员补偿申请。
     *
     * @param snapshot 团快照
     * @param refundScene 退款场景
     * @param reason 退款原因
     * @param eventTime 事件时间
     * @return 成功投递的消息数量
     */
    private int requestRefundForPendingMembers(GroupCacheSnapshot snapshot, String refundScene, String reason, Date eventTime) {
        if (snapshot == null || snapshot.getInstance() == null || snapshot.getMembers() == null) {
            return 0;
        }
        int publishedCount = 0;
        for (GroupMember member : snapshot.getMembers()) {
            if (requestRefundForPendingMember(snapshot, member, refundScene, reason, eventTime)) {
                publishedCount++;
            }
        }
        return publishedCount;
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
    private boolean requestRefundForPendingMember(GroupCacheSnapshot snapshot, GroupMember member,
                                                  String refundScene, String reason, Date eventTime) {
        if (member == null
                || !GroupMemberBizStatus.FAILED_REFUND_PENDING.name().equals(member.getMemberStatus())) {
            return false;
        }
        if (!hasText(member.getOrderId()) || member.getPayAmount() == null) {
            log.error("拼团退款补偿消息未发送，成员关键数据缺失: groupId={}, orderId={}, memberStatus={}",
                    snapshot.getInstance().getId(), member.getOrderId(), member.getMemberStatus());
            return false;
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
            return true;
        } catch (RuntimeException e) {
            log.error("拼团退款补偿消息发送失败: groupId={}, orderId={}, refundScene={}",
                    refundMessage.getGroupId(), refundMessage.getOrderId(), refundScene, e);
            groupStorageComponent.submitCompensationTask(
                    GroupCompensationTaskType.REFUND_RETRY,
                    refundMessage.getGroupId(),
                    refundMessage.getEventTime(),
                    e.getMessage()
            );
            return false;
        }
    }

    /**
     * 判断当前快照中是否存在待退款成员。
     *
     * @param snapshot 拼团快照
     * @return true-存在待退款成员
     */
    private boolean hasPendingRefundMembers(GroupCacheSnapshot snapshot) {
        return snapshot != null
                && snapshot.getMembers() != null
                && snapshot.getMembers().stream()
                .anyMatch(member -> member != null
                        && GroupMemberBizStatus.FAILED_REFUND_PENDING.name().equals(member.getMemberStatus()));
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
