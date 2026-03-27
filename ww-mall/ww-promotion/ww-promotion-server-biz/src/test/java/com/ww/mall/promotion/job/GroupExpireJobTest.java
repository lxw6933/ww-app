package com.ww.mall.promotion.job;

import com.ww.mall.promotion.component.GroupStorageComponent;
import com.ww.mall.promotion.constants.GroupBizConstants;
import com.ww.mall.promotion.engine.GroupCommandService;
import com.ww.mall.promotion.engine.model.GroupCompensationTaskSnapshot;
import com.ww.mall.promotion.enums.GroupCompensationTaskType;
import com.ww.mall.promotion.service.group.GroupActivityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 拼团过期任务测试。
 *
 * @author ww
 * @create 2026-03-26
 * @description: 校验过期任务会在单次调度内连续处理多个批次
 */
@ExtendWith(MockitoExtension.class)
class GroupExpireJobTest {

    @Mock
    private GroupStorageComponent groupStorageComponent;

    @Mock
    private GroupCommandService groupCommandService;

    @Mock
    private GroupActivityService groupActivityService;

    @InjectMocks
    private GroupExpireJob groupExpireJob;

    /**
     * 当首批数量达到批次上限时，任务应继续拉取下一批，直到窗口内数据被吃完。
     */
    @Test
    void shouldContinueProcessingMultipleBatchesUntilWindowDrained() {
        Set<String> firstBatch = buildGroupIds(GroupBizConstants.EXPIRE_JOB_BATCH_LIMIT);
        Set<String> secondBatch = new LinkedHashSet<>(Collections.singletonList("group-last"));
        when(groupStorageComponent.findExpiredGroupIds(anyLong(), eq((long) GroupBizConstants.EXPIRE_JOB_BATCH_LIMIT)))
                .thenReturn(firstBatch)
                .thenReturn(secondBatch);

        groupExpireJob.groupExpireJobHandler();

        verify(groupStorageComponent, times(2))
                .findExpiredGroupIds(anyLong(), eq((long) GroupBizConstants.EXPIRE_JOB_BATCH_LIMIT));
        verify(groupCommandService, times(GroupBizConstants.EXPIRE_JOB_BATCH_LIMIT + 1))
                .expireGroup(org.mockito.ArgumentMatchers.anyString(), eq("拼团过期未成团"));
    }

    /**
     * 当没有到期拼团时，任务应直接结束。
     */
    @Test
    void shouldStopImmediatelyWhenNoExpiredGroups() {
        when(groupStorageComponent.findExpiredGroupIds(anyLong(), eq((long) GroupBizConstants.EXPIRE_JOB_BATCH_LIMIT)))
                .thenReturn(Collections.emptySet());

        groupExpireJob.groupExpireJobHandler();

        verify(groupStorageComponent, times(1))
                .findExpiredGroupIds(anyLong(), eq((long) GroupBizConstants.EXPIRE_JOB_BATCH_LIMIT));
        verify(groupCommandService, times(0))
                .expireGroup(org.mockito.ArgumentMatchers.anyString(), eq("拼团过期未成团"));
    }

    /**
     * 活动状态任务入口应复用为活动统计归档任务。
     */
    @Test
    void shouldSettleExpiredActivityStatisticsWhenTriggeringActivityStatusJob() {
        groupExpireJob.activityStatusUpdateJobHandler();

        verify(groupActivityService, times(1)).settleExpiredActivityStatistics();
    }

    /**
     * 补偿任务执行成功后，应从补偿索引中移除。
     */
    @Test
    void shouldRemoveCompensationTaskWhenSyncJobSucceeds() {
        GroupCompensationTaskSnapshot task = buildCompensationTask("task-1", GroupCompensationTaskType.PROJECTION_SYNC);
        when(groupStorageComponent.findDueCompensationTasks(anyLong(), eq((long) GroupBizConstants.COMPENSATION_JOB_BATCH_LIMIT)))
                .thenReturn(Collections.singletonList(task));

        groupExpireJob.groupSyncToMongoJobHandler();

        verify(groupStorageComponent, times(1)).syncProjection("group-1");
        verify(groupStorageComponent, times(1)).removeCompensationTask("task-1");
    }

    /**
     * 补偿任务执行失败后，应重排重试。
     */
    @Test
    void shouldRetryCompensationTaskWhenSyncJobFails() {
        GroupCompensationTaskSnapshot task = buildCompensationTask("task-2", GroupCompensationTaskType.PROJECTION_SYNC);
        when(groupStorageComponent.findDueCompensationTasks(anyLong(), eq((long) GroupBizConstants.COMPENSATION_JOB_BATCH_LIMIT)))
                .thenReturn(Collections.singletonList(task));
        when(groupStorageComponent.syncProjection("group-1")).thenThrow(new RuntimeException("mongo down"));

        groupExpireJob.groupSyncToMongoJobHandler();

        verify(groupStorageComponent, times(1)).retryCompensationTask(eq(task), anyString());
    }

    /**
     * 退款补偿任务应复用命令服务重发退款补偿。
     */
    @Test
    void shouldTriggerRefundCompensationWhenProcessingRefundRetryTask() {
        GroupCompensationTaskSnapshot task = buildCompensationTask("task-3", GroupCompensationTaskType.REFUND_RETRY);
        when(groupStorageComponent.findDueCompensationTasks(anyLong(), eq((long) GroupBizConstants.COMPENSATION_JOB_BATCH_LIMIT)))
                .thenReturn(Collections.singletonList(task));

        groupExpireJob.groupSyncToMongoJobHandler();

        verify(groupCommandService, times(1))
                .triggerPendingRefundCompensation("group-1", "定时任务触发拼团退款补偿");
        verify(groupStorageComponent, times(1)).removeCompensationTask("task-3");
    }

    /**
     * 构建指定数量的团ID集合。
     *
     * @param size 数量
     * @return 团ID集合
     */
    private Set<String> buildGroupIds(int size) {
        Set<String> groupIds = new LinkedHashSet<>();
        for (int index = 0; index < size; index++) {
            groupIds.add("group-" + index);
        }
        return groupIds;
    }

    /**
     * 构造补偿任务。
     *
     * @param taskId 任务ID
     * @param taskType 任务类型
     * @return 补偿任务
     */
    private GroupCompensationTaskSnapshot buildCompensationTask(String taskId, GroupCompensationTaskType taskType) {
        GroupCompensationTaskSnapshot task = new GroupCompensationTaskSnapshot();
        task.setTaskId(taskId);
        task.setTaskType(taskType);
        task.setGroupId("group-1");
        task.setRetryCount(0);
        task.setStatus("PENDING");
        return task;
    }
}
