package com.ww.mall.promotion.job;

import com.ww.mall.promotion.component.GroupStorageComponent;
import com.ww.mall.promotion.constants.GroupBizConstants;
import com.ww.mall.promotion.engine.GroupCommandService;
import com.ww.mall.promotion.service.group.GroupActivityService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;

/**
 * 拼团过期补偿任务。
 * <p>
 * 当前任务仅负责扫描“已到期但仍处于 OPEN 状态”的拼团，并调用统一命令服务完成关团。
 * 活动状态已改为按开始/结束时间实时推导，因此不再依赖额外的活动状态滚动任务。
 *
 * @author ww
 * @create 2026-03-19
 * @description: 负责处理拼团过期补偿
 */
@Slf4j
@Component
public class GroupExpireJob {

    @Resource
    private GroupStorageComponent groupStorageComponent;

    @Resource
    private GroupCommandService groupCommandService;

    @Resource
    private GroupActivityService groupActivityService;

    /**
     * 处理过期拼团。
     * <p>
     * 单次调度会连续拉取多个批次的到期团，尽量在一个调度窗口内吃掉更多积压；
     * 每个团的最终状态迁移仍由 Redis Lua 保证原子性和幂等性，因此多实例并发执行不会造成重复关团。
     */
    @XxlJob("groupExpireJobHandler")
    public void groupExpireJobHandler() {
        long nowMillis = System.currentTimeMillis();
        for (int round = 0; round < GroupBizConstants.EXPIRE_JOB_MAX_ROUNDS; round++) {
            Set<String> expiredGroupIds = groupStorageComponent.findExpiredGroupIds(
                    nowMillis,
                    GroupBizConstants.EXPIRE_JOB_BATCH_LIMIT
            );
            if (expiredGroupIds.isEmpty()) {
                return;
            }
            for (String groupId : expiredGroupIds) {
                try {
                    groupCommandService.expireGroup(groupId, "拼团过期未成团");
                } catch (Exception e) {
                    log.error("处理过期拼团失败: groupId={}", groupId, e);
                }
            }
            if (expiredGroupIds.size() < GroupBizConstants.EXPIRE_JOB_BATCH_LIMIT) {
                return;
            }
        }
    }

    /**
     * 兼容保留空任务。
     * <p>
     * Mongo 投影当前由 `group.state.changed` 内部消息异步驱动。
     * 这里继续保留空实现，仅用于兼容已有 XXL-Job 配置，避免任务中心继续调度时报错。
     */
    @XxlJob("groupSyncToMongoJobHandler")
    public void groupSyncToMongoJobHandler() {
    }

    /**
     * 兼容保留活动状态任务入口。
     * <p>
     * 旧版本依赖该任务滚动更新活动 status 字段。
     * 当前改为复用该入口执行“已结束活动统计归档”，将 Redis 中的最终活动统计落库后再删除 Redis Key。
     */
    @XxlJob("activityStatusUpdateJobHandler")
    public void activityStatusUpdateJobHandler() {
        groupActivityService.settleExpiredActivityStatistics();
    }
}
