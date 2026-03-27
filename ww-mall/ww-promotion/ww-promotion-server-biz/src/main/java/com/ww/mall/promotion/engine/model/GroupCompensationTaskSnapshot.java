package com.ww.mall.promotion.engine.model;

import com.ww.mall.promotion.enums.GroupCompensationTaskType;
import lombok.Data;

/**
 * 拼团补偿任务快照。
 * <p>
 * 该对象序列化到 Redis，用于承接“主状态已变更，但副作用执行失败”的补偿任务。
 *
 * @author ww
 * @create 2026-03-27
 * @description: 拼团补偿任务快照
 */
@Data
public class GroupCompensationTaskSnapshot {

    /**
     * 任务ID。
     */
    private String taskId;

    /**
     * 任务类型。
     */
    private GroupCompensationTaskType taskType;

    /**
     * 拼团ID。
     */
    private String groupId;

    /**
     * 首次事件时间毫秒值。
     */
    private Long eventTime;

    /**
     * 当前重试次数。
     */
    private Integer retryCount;

    /**
     * 下次执行时间毫秒值。
     */
    private Long nextRetryTime;

    /**
     * 最近一次错误信息。
     */
    private String lastError;

    /**
     * 任务状态：PENDING/DEAD。
     */
    private String status;

    /**
     * 创建时间毫秒值。
     */
    private Long createTime;

    /**
     * 更新时间毫秒值。
     */
    private Long updateTime;
}
