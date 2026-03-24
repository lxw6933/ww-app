package com.ww.mall.promotion.engine.notify;

import com.ww.app.rabbitmq.RabbitMqPublisher;
import com.ww.mall.promotion.constants.GroupBizConstants;
import com.ww.mall.promotion.engine.model.GroupCacheSnapshot;
import com.ww.mall.promotion.engine.projection.GroupProjectionPersistenceService;
import com.ww.mall.promotion.entity.group.GroupMember;
import com.ww.mall.promotion.entity.group.GroupNotifyTask;
import com.ww.mall.promotion.enums.GroupMemberBizStatus;
import com.ww.mall.promotion.enums.GroupNotifyEventType;
import com.ww.mall.promotion.enums.GroupNotifyTaskStatus;
import com.ww.mall.promotion.mq.GroupFailedMessage;
import com.ww.mall.promotion.mq.GroupMqConstant;
import com.ww.mall.promotion.mq.GroupRefundMessage;
import com.ww.mall.promotion.mq.GroupSuccessMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 拼团通知任务服务。
 * <p>
 * 该服务负责：
 * 1. 在拼团进入“需要对外通知”的最终状态后创建通知任务。
 * 2. 先同步直发业务 MQ，失败时落任务重试。
 * 3. 通过定时任务补偿发送失败或发送中断的任务。
 *
 * @author ww
 * @create 2026-03-24
 * @description: 拼团业务 MQ 通知任务服务
 */
@Slf4j
@Service
public class GroupNotifyTaskService {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private GroupProjectionPersistenceService groupProjectionPersistenceService;

    @Resource
    private RabbitMqPublisher rabbitMqPublisher;

    /**
     * 当快照处于最终通知态时，尝试直发并写入重试任务。
     *
     * @param snapshot 拼团快照
     */
    public void notifyIfNecessary(GroupCacheSnapshot snapshot) {
        GroupNotifyEventType eventType = resolveNotifyEventType(snapshot);
        if (eventType == null) {
            return;
        }
        GroupNotifyTask task = prepareTask(snapshot, eventType);
        if (task == null || GroupNotifyTaskStatus.SUCCESS.name().equals(task.getNotifyStatus())
                || GroupNotifyTaskStatus.DEAD.name().equals(task.getNotifyStatus())) {
            return;
        }
        triggerTask(task.getId(), snapshot);
    }

    /**
     * 定时补偿发送失败或发送中断的通知任务。
     */
    @Scheduled(fixedDelay = GroupBizConstants.GROUP_NOTIFY_TASK_FIXED_DELAY_MILLIS)
    public void retryNotifyTasks() {
        Date now = new Date();
        Query query = GroupNotifyTask.buildDueTaskQuery(now);
        query.limit(GroupBizConstants.GROUP_NOTIFY_TASK_BATCH_SIZE);
        List<GroupNotifyTask> tasks = mongoTemplate.find(query, GroupNotifyTask.class);
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        for (GroupNotifyTask task : tasks) {
            if (task == null || !hasText(task.getId())) {
                continue;
            }
            triggerTask(task.getId(), null);
        }
    }

    /**
     * 触发指定任务的发送。
     *
     * @param taskId 任务ID
     * @param preferredSnapshot 调用方已持有的最新快照，可为空
     */
    private void triggerTask(String taskId, GroupCacheSnapshot preferredSnapshot) {
        Date now = new Date();
        GroupNotifyTask claimedTask = claimTask(taskId, now);
        if (claimedTask == null) {
            return;
        }
        try {
            GroupCacheSnapshot snapshot = preferredSnapshot != null ? preferredSnapshot
                    : groupProjectionPersistenceService.loadSnapshotFromMongo(claimedTask.getGroupId());
            if (snapshot == null || snapshot.getInstance() == null) {
                throw new IllegalStateException("拼团通知任务缺少可用快照");
            }
            publish(snapshot, GroupNotifyEventType.valueOf(claimedTask.getEventType()));
            markSuccess(taskId, now);
        } catch (Exception e) {
            markFailure(claimedTask, e, now);
        }
    }

    /**
     * 预创建或刷新通知任务。
     *
     * @param snapshot 拼团快照
     * @param eventType 事件类型
     * @return 任务文档
     */
    private GroupNotifyTask prepareTask(GroupCacheSnapshot snapshot, GroupNotifyEventType eventType) {
        String taskId = buildTaskId(snapshot, eventType);
        if (!hasText(taskId)) {
            return null;
        }
        Date now = new Date();
        GroupNotifyTask task = mongoTemplate.findOne(GroupNotifyTask.buildIdQuery(taskId), GroupNotifyTask.class);
        if (task == null) {
            task = new GroupNotifyTask();
            task.setId(taskId);
            task.setCreateTime(now);
            task.setRetryCount(0);
        }
        if (GroupNotifyTaskStatus.SUCCESS.name().equals(task.getNotifyStatus())
                || GroupNotifyTaskStatus.DEAD.name().equals(task.getNotifyStatus())) {
            return task;
        }
        task.setGroupId(snapshot.getInstance().getId());
        task.setEventType(eventType.name());
        task.setNotifyStatus(GroupNotifyTaskStatus.INIT.name());
        task.setNextRetryTime(now);
        task.setLastError("");
        task.setUpdateTime(now);
        mongoTemplate.save(task);
        return task;
    }

    /**
     * 领取待发送任务。
     * <p>
     * 领取成功后会把任务状态置为 {@code SENDING}，并写入短租约，
     * 使得实例在发送过程中崩溃后，后续调度仍可在租约超时后重新接管。
     *
     * @param taskId 任务ID
     * @param now 当前时间
     * @return 已领取的任务，领取失败返回 {@code null}
     */
    private GroupNotifyTask claimTask(String taskId, Date now) {
        Date leaseExpireTime = new Date(now.getTime() + GroupBizConstants.GROUP_NOTIFY_TASK_SENDING_LEASE_MILLIS);
        return mongoTemplate.findAndModify(
                GroupNotifyTask.buildClaimQuery(taskId, now),
                GroupNotifyTask.buildSendingUpdate(now, leaseExpireTime),
                FindAndModifyOptions.options().returnNew(true),
                GroupNotifyTask.class
        );
    }

    /**
     * 标记任务发送成功。
     *
     * @param taskId 任务ID
     * @param now 当前时间
     */
    private void markSuccess(String taskId, Date now) {
        mongoTemplate.updateFirst(
                GroupNotifyTask.buildIdQuery(taskId),
                GroupNotifyTask.buildSuccessUpdate(now),
                GroupNotifyTask.class
        );
    }

    /**
     * 标记任务发送失败。
     *
     * @param task 任务文档
     * @param e 失败异常
     * @param now 当前时间
     */
    private void markFailure(GroupNotifyTask task, Exception e, Date now) {
        int currentRetryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
        int nextRetryCount = currentRetryCount + 1;
        String lastError = extractErrorMessage(e);
        if (nextRetryCount >= GroupBizConstants.GROUP_NOTIFY_TASK_MAX_RETRY_COUNT) {
            mongoTemplate.updateFirst(
                    GroupNotifyTask.buildIdQuery(task.getId()),
                    GroupNotifyTask.buildDeadUpdate(now, nextRetryCount, lastError),
                    GroupNotifyTask.class
            );
            log.error("拼团通知任务进入死信: taskId={}, groupId={}, eventType={}, error={}",
                    task.getId(), task.getGroupId(), task.getEventType(), lastError, e);
            return;
        }
        Date nextRetryTime = new Date(now.getTime() + calculateRetryDelayMillis(nextRetryCount));
        mongoTemplate.updateFirst(
                GroupNotifyTask.buildIdQuery(task.getId()),
                GroupNotifyTask.buildFailedUpdate(now, nextRetryCount, nextRetryTime, lastError),
                GroupNotifyTask.class
        );
        log.error("拼团通知任务发送失败，等待重试: taskId={}, groupId={}, eventType={}, retryCount={}, nextRetryTime={}",
                task.getId(), task.getGroupId(), task.getEventType(), nextRetryCount, nextRetryTime, e);
    }

    /**
     * 根据快照判断是否需要对外通知。
     *
     * @param snapshot 拼团快照
     * @return 事件类型，无需通知时返回 {@code null}
     */
    private GroupNotifyEventType resolveNotifyEventType(GroupCacheSnapshot snapshot) {
        if (snapshot == null || snapshot.getInstance() == null || !hasText(snapshot.getInstance().getStatus())) {
            return null;
        }
        if ("SUCCESS".equals(snapshot.getInstance().getStatus()) && snapshot.getInstance().getCompleteTime() != null) {
            return GroupNotifyEventType.GROUP_COMPLETED;
        }
        if ("FAILED".equals(snapshot.getInstance().getStatus()) && snapshot.getInstance().getFailedTime() != null) {
            return GroupNotifyEventType.GROUP_FAILED;
        }
        return null;
    }

    /**
     * 构建任务ID。
     * <p>
     * 任务ID同时作为业务幂等键，保证同一个最终事件只会产生一条通知任务。
     *
     * @param snapshot 拼团快照
     * @param eventType 事件类型
     * @return 任务ID
     */
    private String buildTaskId(GroupCacheSnapshot snapshot, GroupNotifyEventType eventType) {
        if (snapshot == null || snapshot.getInstance() == null || eventType == null) {
            return null;
        }
        Date eventTime = eventType == GroupNotifyEventType.GROUP_COMPLETED
                ? snapshot.getInstance().getCompleteTime()
                : snapshot.getInstance().getFailedTime();
        if (eventTime == null || !hasText(snapshot.getInstance().getId())) {
            return null;
        }
        return snapshot.getInstance().getId() + ":" + eventType.name() + ":" + eventTime.getTime();
    }

    /**
     * 发送拼团业务 MQ。
     *
     * @param snapshot 拼团快照
     * @param eventType 事件类型
     */
    private void publish(GroupCacheSnapshot snapshot, GroupNotifyEventType eventType) {
        if (eventType == GroupNotifyEventType.GROUP_COMPLETED) {
            publishCompletedMessage(snapshot);
            return;
        }
        if (eventType == GroupNotifyEventType.GROUP_FAILED) {
            publishFailedMessages(snapshot);
        }
    }

    /**
     * 发布成团成功消息。
     *
     * @param snapshot 拼团快照
     */
    private void publishCompletedMessage(GroupCacheSnapshot snapshot) {
        GroupSuccessMessage message = new GroupSuccessMessage();
        message.setGroupId(snapshot.getInstance().getId());
        message.setActivityId(snapshot.getInstance().getActivityId());
        message.setCompleteTime(snapshot.getInstance().getCompleteTime());
        message.setMemberOrders(snapshot.getMembers().stream()
                .filter(member -> GroupMemberBizStatus.SUCCESS.name().equals(member.getMemberStatus()))
                .map(this::buildSuccessOrder)
                .collect(Collectors.toList()));
        rabbitMqPublisher.sendMsg(GroupMqConstant.GROUP_EXCHANGE, GroupMqConstant.GROUP_SUCCESS_KEY, message);
    }

    /**
     * 发布拼团失败与退款消息。
     *
     * @param snapshot 拼团快照
     */
    private void publishFailedMessages(GroupCacheSnapshot snapshot) {
        GroupFailedMessage failedMessage = new GroupFailedMessage();
        failedMessage.setGroupId(snapshot.getInstance().getId());
        failedMessage.setActivityId(snapshot.getInstance().getActivityId());
        failedMessage.setFailedTime(snapshot.getInstance().getFailedTime());
        failedMessage.setReason(snapshot.getInstance().getFailReason());
        rabbitMqPublisher.sendMsg(GroupMqConstant.GROUP_EXCHANGE, GroupMqConstant.GROUP_FAILED_KEY, failedMessage);

        List<GroupRefundMessage.RefundOrder> refundOrders = snapshot.getMembers().stream()
                .filter(member -> GroupMemberBizStatus.FAILED_REFUND_PENDING.name().equals(member.getMemberStatus()))
                .map(this::buildRefundOrder)
                .collect(Collectors.toList());
        if (refundOrders.isEmpty()) {
            return;
        }

        GroupRefundMessage refundMessage = new GroupRefundMessage();
        refundMessage.setGroupId(snapshot.getInstance().getId());
        refundMessage.setActivityId(snapshot.getInstance().getActivityId());
        refundMessage.setReason(snapshot.getInstance().getFailReason());
        refundMessage.setRefundOrders(refundOrders);
        rabbitMqPublisher.sendMsg(GroupMqConstant.GROUP_EXCHANGE, GroupMqConstant.GROUP_REFUND_KEY, refundMessage);
    }

    /**
     * 构建成功订单信息。
     *
     * @param member 成员
     * @return 成功订单
     */
    private GroupSuccessMessage.MemberOrder buildSuccessOrder(GroupMember member) {
        GroupSuccessMessage.MemberOrder order = new GroupSuccessMessage.MemberOrder();
        order.setUserId(member.getUserId());
        order.setOrderId(member.getOrderId());
        order.setIsLeader(member.getIsLeader() != null && member.getIsLeader() == 1);
        return order;
    }

    /**
     * 构建退款订单信息。
     *
     * @param member 成员
     * @return 退款订单
     */
    private GroupRefundMessage.RefundOrder buildRefundOrder(GroupMember member) {
        GroupRefundMessage.RefundOrder refundOrder = new GroupRefundMessage.RefundOrder();
        refundOrder.setUserId(member.getUserId());
        refundOrder.setOrderId(member.getOrderId());
        refundOrder.setRefundAmount(member.getPayAmount() != null ? member.getPayAmount() : BigDecimal.ZERO);
        refundOrder.setIsLeader(member.getIsLeader() != null && member.getIsLeader() == 1);
        return refundOrder;
    }

    /**
     * 计算退避重试间隔。
     *
     * @param retryCount 最新重试次数
     * @return 重试等待毫秒数
     */
    private long calculateRetryDelayMillis(int retryCount) {
        if (retryCount <= 1) {
            return 10_000L;
        }
        if (retryCount == 2) {
            return 30_000L;
        }
        if (retryCount == 3) {
            return 60_000L;
        }
        if (retryCount == 4) {
            return 300_000L;
        }
        return 1_800_000L;
    }

    /**
     * 提取异常信息。
     *
     * @param e 异常
     * @return 失败原因
     */
    private String extractErrorMessage(Exception e) {
        if (e == null) {
            return "";
        }
        String message = e.getMessage();
        if (hasText(message)) {
            return message;
        }
        return e.getClass().getSimpleName();
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
