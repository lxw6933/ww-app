package com.ww.mall.promotion.entity.group;

import com.ww.app.mongodb.common.BaseDoc;
import com.ww.mall.promotion.enums.GroupNotifyTaskStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 拼团业务通知任务。
 * <p>
 * 当拼团主状态已经由 Redis Lua 成功落地后，业务 MQ 的发送由该任务表负责兜底重试，
 * 避免“同步直发失败后没有任何补偿抓手”的问题。
 *
 * @author ww
 * @create 2026-03-24
 * @description: 拼团通知任务文档
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document("group_notify_task")
@CompoundIndexes({
        /**
         * 支撑定时任务按状态 + 下一次重试时间扫描到期任务，避免任务量增长后出现全表扫描。
         */
        @CompoundIndex(name = "idx_notify_status_next_retry_time", def = "{'notifyStatus': 1, 'nextRetryTime': 1}")
})
public class GroupNotifyTask extends BaseDoc {

    /**
     * 拼团ID。
     */
    private String groupId;

    /**
     * 领域事件类型。
     */
    private String eventType;

    /**
     * 通知任务状态。
     */
    private String notifyStatus;

    /**
     * 已重试次数。
     */
    private Integer retryCount;

    /**
     * 下次允许重试的时间。
     */
    private Date nextRetryTime;

    /**
     * 最近一次失败原因。
     */
    private String lastError;

    /**
     * 最近一次发送成功时间。
     */
    private Date sentTime;

    /**
     * 构建待重试任务查询。
     * <p>
     * 这里同时包含 {@code INIT}、{@code FAILED} 和超时的 {@code SENDING}，
     * 以便处理实例在发送过程中崩溃后遗留的“发送中”任务。
     *
     * @param now 当前时间
     * @return Mongo 查询
     */
    public static Query buildDueTaskQuery(Date now) {
        List<String> statuses = Arrays.asList(
                GroupNotifyTaskStatus.INIT.name(),
                GroupNotifyTaskStatus.FAILED.name(),
                GroupNotifyTaskStatus.SENDING.name()
        );
        return new Query().addCriteria(
                Criteria.where("notifyStatus").in(statuses)
                        .and("nextRetryTime").lte(now)
        ).with(Sort.by(Sort.Direction.ASC, "nextRetryTime", "id"));
    }

    /**
     * 构建领取待发送任务的条件。
     *
     * @param taskId 任务ID
     * @param now 当前时间
     * @return Mongo 查询
     */
    public static Query buildClaimQuery(String taskId, Date now) {
        List<String> statuses = Arrays.asList(
                GroupNotifyTaskStatus.INIT.name(),
                GroupNotifyTaskStatus.FAILED.name(),
                GroupNotifyTaskStatus.SENDING.name()
        );
        return BaseDoc.buildIdQuery(taskId).addCriteria(
                Criteria.where("notifyStatus").in(statuses)
                        .and("nextRetryTime").lte(now)
        );
    }

    /**
     * 构建“任务发送中”的状态更新。
     *
     * @param now 当前时间
     * @param leaseExpireTime 发送租约过期时间
     * @return Mongo 更新
     */
    public static Update buildSendingUpdate(Date now, Date leaseExpireTime) {
        return new Update()
                .set("notifyStatus", GroupNotifyTaskStatus.SENDING.name())
                .set("nextRetryTime", leaseExpireTime)
                .set("updateTime", now);
    }

    /**
     * 构建发送成功更新。
     *
     * @param now 当前时间
     * @return Mongo 更新
     */
    public static Update buildSuccessUpdate(Date now) {
        return new Update()
                .set("notifyStatus", GroupNotifyTaskStatus.SUCCESS.name())
                .set("sentTime", now)
                .set("lastError", "")
                .set("updateTime", now);
    }

    /**
     * 构建发送失败更新。
     *
     * @param now 当前时间
     * @param retryCount 最新重试次数
     * @param nextRetryTime 下次重试时间
     * @param lastError 最近失败原因
     * @return Mongo 更新
     */
    public static Update buildFailedUpdate(Date now, int retryCount, Date nextRetryTime, String lastError) {
        return new Update()
                .set("notifyStatus", GroupNotifyTaskStatus.FAILED.name())
                .set("retryCount", retryCount)
                .set("nextRetryTime", nextRetryTime)
                .set("lastError", lastError)
                .set("updateTime", now);
    }

    /**
     * 构建死信更新。
     *
     * @param now 当前时间
     * @param retryCount 最新重试次数
     * @param lastError 最近失败原因
     * @return Mongo 更新
     */
    public static Update buildDeadUpdate(Date now, int retryCount, String lastError) {
        return new Update()
                .set("notifyStatus", GroupNotifyTaskStatus.DEAD.name())
                .set("retryCount", retryCount)
                .set("lastError", lastError)
                .set("updateTime", now);
    }
}
