package com.ww.mall.promotion.job;

import com.ww.mall.promotion.constants.GroupBizConstants;
import com.ww.mall.promotion.engine.GroupCommandService;
import com.ww.mall.promotion.component.GroupStorageComponent;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;

/**
 * 拼团任务。
 * <p>
 * 当前仅保留“过期未成团自动关团”任务。
 * 活动状态已经改为基于开始时间、结束时间实时推导，因此不再需要单独维护活动状态任务。
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

    /**
     * 处理过期拼团。
     * <p>
     * 该任务只扫描过期索引中到期的团，并调用统一命令服务执行失败关团逻辑。
     * 即便多实例并发执行，最终状态迁移仍由 Redis Lua 保证原子性与幂等性。
     */
    @XxlJob("groupExpireJobHandler")
    public void groupExpireJobHandler() {
        Set<String> expiredGroupIds = groupStorageComponent.findExpiredGroupIds(
                System.currentTimeMillis(),
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
     * 新版本 status 已改为实时计算，保留空实现仅用于兼容已有 XXL-Job 配置，避免任务中心继续调度时报错。
     */
    @XxlJob("activityStatusUpdateJobHandler")
    public void activityStatusUpdateJobHandler() {
    }
}
