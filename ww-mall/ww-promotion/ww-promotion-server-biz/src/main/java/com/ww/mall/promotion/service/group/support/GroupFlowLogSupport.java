package com.ww.mall.promotion.service.group.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ww.mall.promotion.entity.group.GroupFlowLog;
import com.ww.mall.promotion.event.GroupEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.UUID;

/**
 * 拼团链路日志支持组件。
 * <p>
 * 该组件以“业务不被日志反向阻塞”为原则，所有写日志动作均采用 best effort，
 * 即便 Mongo 写入失败，也只记录错误日志，不影响拼团主流程。
 *
 * @author ww
 * @create 2026-03-16
 * @description: 统一封装拼团链路日志落库与 traceId 生成
 */
@Slf4j
@Component
public class GroupFlowLogSupport {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private ObjectMapper objectMapper;

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
     * @param eventType 事件类型
     * @param source 日志来源
     * @param status 当前状态
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     * @param payload 请求或事件快照
     */
    public void record(String traceId, String groupId, String activityId, Long userId, String orderId,
                       String eventType, String source, String status,
                       String errorCode, String errorMessage, Object payload) {
        try {
            GroupFlowLog flowLog = new GroupFlowLog();
            Date now = new Date();
            flowLog.setTraceId(traceId);
            flowLog.setGroupId(groupId);
            flowLog.setActivityId(activityId);
            flowLog.setUserId(userId);
            flowLog.setOrderId(orderId);
            flowLog.setEventType(eventType);
            flowLog.setSource(source);
            flowLog.setStatus(status);
            flowLog.setRetryCount(0);
            flowLog.setErrorCode(errorCode);
            flowLog.setErrorMessage(errorMessage);
            flowLog.setPayloadSnapshot(toJson(payload));
            flowLog.setCreateTime(now);
            flowLog.setUpdateTime(now);
            mongoTemplate.save(flowLog);
        } catch (Exception e) {
            log.error("记录拼团链路日志失败: traceId={}, groupId={}, eventType={}, source={}",
                    traceId, groupId, eventType, source, e);
        }
    }

    /**
     * 按事件对象记录链路日志。
     *
     * @param event 拼团事件
     * @param source 日志来源
     * @param status 当前状态
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     */
    public void recordEvent(GroupEvent event, String source, String status, String errorCode, String errorMessage) {
        if (event == null) {
            record(createTraceId(), null, null, null, null,
                    "UNKNOWN_EVENT", source, status, errorCode, errorMessage, null);
            return;
        }
        record(event.getTraceId(), event.getGroupId(), event.getActivityId(), event.getUserId(), event.getOrderId(),
                event.getEventType() != null ? event.getEventType().name() : "UNKNOWN_EVENT",
                source, status, errorCode, errorMessage, event);
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
