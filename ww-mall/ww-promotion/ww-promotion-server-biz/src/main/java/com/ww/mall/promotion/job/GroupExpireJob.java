package com.ww.mall.promotion.job;

import com.ww.mall.promotion.constants.GroupBizConstants;
import com.ww.mall.promotion.engine.GroupCommandService;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.enums.GroupActivityStatus;
import com.ww.mall.promotion.key.GroupRedisKeyBuilder;
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
 * 拼团过期与活动状态任务。
 *
 * @author ww
 * @create 2026-03-19
 * @description: 仅保留过期关团和活动状态滚动更新
 */
@Slf4j
@Component
public class GroupExpireJob {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private GroupRedisKeyBuilder groupRedisKeyBuilder;

    @Resource
    private GroupCommandService groupCommandService;

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 处理过期拼团。
     */
    @XxlJob("groupExpireJobHandler")
    public void groupExpireJobHandler() {
        Set<String> expiredGroupIds = stringRedisTemplate.opsForZSet().rangeByScore(
                groupRedisKeyBuilder.buildExpiryIndexKey(),
                0,
                System.currentTimeMillis(),
                0,
                GroupBizConstants.EXPIRE_JOB_BATCH_LIMIT
        );
        if (expiredGroupIds == null || expiredGroupIds.isEmpty()) {
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
     * 兼容保留空任务，新的 Mongo 投影由 Redis Stream 投影器常驻处理。
     */
    @XxlJob("groupSyncToMongoJobHandler")
    public void groupSyncToMongoJobHandler() {
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
