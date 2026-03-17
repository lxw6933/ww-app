package com.ww.mall.promotion.job;

import com.ww.mall.promotion.constants.GroupBizConstants;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.enums.GroupActivityStatus;
import com.ww.mall.promotion.key.GroupRedisKeyBuilder;
import com.ww.mall.promotion.service.group.GroupInstanceService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Set;

/**
 * 拼团过期任务。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 处理未成团超时关闭和活动状态滚动更新
 */
@Slf4j
@Component
public class GroupExpireJob {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private GroupRedisKeyBuilder groupRedisKeyBuilder;

    @Resource
    private GroupInstanceService groupInstanceService;

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 处理过期拼团。
     */
    @XxlJob("groupExpireJobHandler")
    public void groupExpireJobHandler() {
        String expiryIndexKey = groupRedisKeyBuilder.buildExpiryIndexKey();
        long nowMillis = System.currentTimeMillis();
        Set<String> expiredGroupIds = stringRedisTemplate.opsForZSet()
                .rangeByScore(expiryIndexKey, 0, nowMillis, 0, GroupBizConstants.EXPIRE_JOB_BATCH_LIMIT);
        if (expiredGroupIds == null || expiredGroupIds.isEmpty()) {
            return;
        }
        for (String groupId : expiredGroupIds) {
            try {
                groupInstanceService.handleGroupFailed(groupId);
            } catch (Exception e) {
                log.error("处理过期拼团失败: groupId={}", groupId, e);
            }
        }
    }

    /**
     * 同步任务保留兼容入口，实际改为预热 Redis。
     */
    @XxlJob("groupSyncToMongoJobHandler")
    public void groupSyncToMongoJobHandler() {
        String expiryIndexKey = groupRedisKeyBuilder.buildExpiryIndexKey();
        Set<String> groupIds = stringRedisTemplate.opsForZSet().range(expiryIndexKey, 0, GroupBizConstants.SYNC_JOB_BATCH_LIMIT - 1L);
        if (groupIds == null || groupIds.isEmpty()) {
            return;
        }
        for (String groupId : groupIds) {
            try {
                groupInstanceService.getGroupDetail(groupId);
            } catch (Exception e) {
                log.warn("预热拼团缓存失败: groupId={}", groupId, e);
            }
        }
    }

    /**
     * 更新活动状态。
     */
    @XxlJob("activityStatusUpdateJobHandler")
    public void activityStatusUpdateJobHandler() {
        Date now = new Date();
        Query startQuery = GroupActivity.buildStatusQuery(GroupActivityStatus.NOT_STARTED.getCode());
        startQuery.addCriteria(Criteria.where("startTime").lte(now).and("endTime").gte(now));
        mongoTemplate.updateMulti(startQuery,
                GroupActivity.buildStatusUpdate(GroupActivityStatus.ACTIVE.getCode()), GroupActivity.class);

        Query endQuery = GroupActivity.buildStatusQuery(GroupActivityStatus.ACTIVE.getCode());
        endQuery.addCriteria(Criteria.where("endTime").lt(now));
        mongoTemplate.updateMulti(endQuery,
                GroupActivity.buildStatusUpdate(GroupActivityStatus.ENDED.getCode()), GroupActivity.class);
    }
}
