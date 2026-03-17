package com.ww.mall.promotion.service.group.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ww.app.mongodb.handler.MongoBulkDataHandler;
import com.ww.mall.promotion.component.GroupFlowLogQueueComponent;
import com.ww.mall.promotion.entity.group.GroupFlowLog;
import com.ww.mall.promotion.enums.GroupFlowSource;
import com.ww.mall.promotion.enums.GroupFlowStage;
import com.ww.mall.promotion.enums.GroupFlowStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Date;
import java.util.UUID;

/**
 * 拼团链路日志支持组件。
 * <p>
 * 该组件以“业务不被日志反向阻塞”为原则，所有写日志动作均采用 best effort，
 * 即便 Mongo 写入失败，也只记录错误日志，不影响拼团主流程。
 * 同时通过阶段策略控制落库噪音：
 * 1. 支付回调、开团/参团入口、成团/失败流转、MQ 边界这类关键检查点允许落库。
 * 2. SAVE_INSTANCE/SAVE_MEMBER 等高频内部成功细节默认不落库，仅保留普通日志与失败记录。
 *
 * @author ww
 * @create 2026-03-16
 * @description: 统一封装拼团链路日志落库与 traceId 生成
 */
@Slf4j
@Component
public class GroupFlowLogSupport {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private MongoBulkDataHandler<GroupFlowLog> mongoBulkDataHandler;

    private GroupFlowLogQueueComponent flowLogQueueComponent;

    /**
     * 初始化批量落库组件。
     */
    @PostConstruct
    public void init() {
        flowLogQueueComponent = new GroupFlowLogQueueComponent(mongoBulkDataHandler);
    }

    /**
     * 关闭前尽量刷出剩余日志。
     */
    @PreDestroy
    public void destroy() {
        if (flowLogQueueComponent != null) {
            flowLogQueueComponent.destroy();
        }
    }

    /**
     * 生成链路追踪ID。
     *
     * @return 32 位无中划线 traceId
     */
    public String createTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 记录拼团链路日志。
     *
     * @param traceId 链路追踪ID
     * @param groupId 拼团实例ID
     * @param activityId 活动ID
     * @param userId 用户ID
     * @param orderId 订单ID
     * @param stage 事件阶段
     * @param source 日志来源
     * @param status 当前状态
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     * @param payload 请求或事件快照
     */
    public void record(String traceId, String groupId, String activityId, Long userId, String orderId,
                       GroupFlowStage stage, GroupFlowSource source, GroupFlowStatus status,
                       String errorCode, String errorMessage, Object payload) {
        if (!shouldPersist(stage, status)) {
            return;
        }
        try {
            GroupFlowLog flowLog = new GroupFlowLog();
            Date now = new Date();
            flowLog.setTraceId(traceId);
            flowLog.setGroupId(groupId);
            flowLog.setActivityId(activityId);
            flowLog.setUserId(userId);
            flowLog.setOrderId(orderId);
            flowLog.setEventType(stage != null ? stage.name() : GroupFlowStage.UNKNOWN_EVENT.name());
            flowLog.setSource(source != null ? source.name() : null);
            flowLog.setStatus(status != null ? status.name() : null);
            flowLog.setRetryCount(0);
            flowLog.setErrorCode(errorCode);
            flowLog.setErrorMessage(errorMessage);
            flowLog.setPayloadSnapshot(toJson(payload));
            flowLog.setCreateTime(now);
            flowLog.setUpdateTime(now);
            flowLogQueueComponent.addRecordToQueue(flowLog);
        } catch (Exception e) {
            log.error("记录拼团链路日志失败: traceId={}, groupId={}, stage={}, source={}",
                    traceId, groupId, stage, source, e);
        }
    }

    /**
     * 记录失败日志。
     * <p>
     * 失败事件默认必须落库，用于排障与补偿回放。
     *
     * @param traceId 链路追踪ID
     * @param groupId 拼团ID
     * @param activityId 活动ID
     * @param userId 用户ID
     * @param orderId 订单ID
     * @param stage 事件阶段
     * @param source 事件来源
     * @param errorMessage 错误信息
     * @param payload 快照
     */
    public void recordFailure(String traceId, String groupId, String activityId, Long userId, String orderId,
                              GroupFlowStage stage, GroupFlowSource source,
                              String errorMessage, Object payload) {
        record(traceId, groupId, activityId, userId, orderId,
                stage, source, GroupFlowStatus.FAILED, null, errorMessage, payload);
    }

    /**
     * 判断当前阶段是否需要落库。
     *
     * @param stage 事件阶段
     * @param status 记录状态
     * @return true-需要落库
     */
    private boolean shouldPersist(GroupFlowStage stage, GroupFlowStatus status) {
        if (status == null) {
            return false;
        }
        if (GroupFlowStatus.FAILED == status) {
            return true;
        }
        GroupFlowStage actualStage = stage != null ? stage : GroupFlowStage.UNKNOWN_EVENT;
        return actualStage.isPersistOnNonFailure();
    }

    /**
     * 序列化请求或事件快照。
     *
     * @param payload 快照对象
     * @return JSON 字符串；序列化失败时返回对象字符串
     */
    private String toJson(Object payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("序列化拼团链路日志快照失败: payloadType={}", payload.getClass().getName(), e);
            return String.valueOf(payload);
        }
    }
}
