package com.ww.mall.promotion.job;

import com.ww.app.redis.component.lua.RedisScriptComponent;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.entity.group.GroupInstance;
import com.ww.mall.promotion.key.GroupRedisKeyBuilder;
import com.ww.mall.promotion.service.group.GroupInstanceService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

import static com.ww.mall.promotion.config.LuaScriptConfiguration.EXPIRE_MARK_FAILED_SCRIPT_NAME;

/**
 * @author ww
 * @create 2025-12-08 18:30
 * @description: 拼团过期定时任务
 */
@Slf4j
@Component
public class GroupExpireJob {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private GroupRedisKeyBuilder groupRedisKeyBuilder;

    @Resource
    private GroupInstanceService instanceService;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private RedisScriptComponent redisScriptComponent;

    /**
     * 处理过期拼团任务
     * 每分钟执行一次
     */
    @XxlJob("groupExpireJobHandler")
    public void groupExpireJobHandler() {
        log.info("开始处理过期拼团任务");
        long startTime = System.currentTimeMillis();

        try {
            String expiryIndexKey = groupRedisKeyBuilder.buildExpiryIndexKey();
            long nowMillis = System.currentTimeMillis();

            // 查询过期的拼团ID（score <= nowMillis）
            Set<String> expiredGroupIds = stringRedisTemplate.opsForZSet()
                    .rangeByScore(expiryIndexKey, 0, nowMillis);

            if (expiredGroupIds == null || expiredGroupIds.isEmpty()) {
                log.info("没有过期的拼团");
                return;
            }

            log.info("发现{}个过期拼团", expiredGroupIds.size());

            int successCount = 0;
            int failCount = 0;

            // 批量处理过期拼团
            for (String groupId : expiredGroupIds) {
                try {
                    // 使用Lua脚本原子性地标记失败（Lua脚本内部获取当前时间）
                    String metaKey = groupRedisKeyBuilder.buildGroupMetaKey(groupId);
                    List<String> keys = Arrays.asList(metaKey, expiryIndexKey);
                    List<String> args = Collections.singletonList(groupId);

                    Long result = redisScriptComponent.executeLuaScript(EXPIRE_MARK_FAILED_SCRIPT_NAME,
                            ReturnType.INTEGER,
                            keys,
                            args
                    );
                    if (result != null && result == 1) {
                        // 标记成功，异步处理失败逻辑
                        instanceService.handleGroupFailed(groupId);
                        successCount++;
                        log.info("处理过期拼团成功: groupId={}", groupId);
                    } else {
                        failCount++;
                        log.warn("处理过期拼团失败: groupId={}, result={}", groupId, result);
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("处理过期拼团异常: groupId={}", groupId, e);
                }
            }

            long endTime = System.currentTimeMillis();
            log.info("过期拼团处理完成: 总数={}, 成功={}, 失败={}, 耗时={}ms",
                    expiredGroupIds.size(), successCount, failCount, (endTime - startTime));

        } catch (Exception e) {
            log.error("处理过期拼团任务异常", e);
        }
    }

    /**
     * 同步Redis数据到MongoDB任务
     * 每小时执行一次
     */
    @XxlJob("groupSyncToMongoJobHandler")
    public void groupSyncToMongoJobHandler() {
        log.info("开始同步拼团数据到MongoDB");
        long startTime = System.currentTimeMillis();
        int syncCount = 0;
        int errorCount = 0;

        try {
            // 从过期索引中获取所有拼团ID（包括未过期的）
            String expiryIndexKey = groupRedisKeyBuilder.buildExpiryIndexKey();
            Set<String> allGroupIds = stringRedisTemplate.opsForZSet().range(expiryIndexKey, 0, -1);

            if (allGroupIds == null || allGroupIds.isEmpty()) {
                log.info("没有需要同步的拼团数据");
                return;
            }

            for (String groupId : allGroupIds) {
                try {
                    syncGroupToMongo(groupId);
                    syncCount++;
                } catch (Exception e) {
                    errorCount++;
                    log.error("同步拼团数据到MongoDB失败: groupId={}", groupId, e);
                }
            }

            long endTime = System.currentTimeMillis();
            log.info("同步拼团数据到MongoDB完成: 总数={}, 成功={}, 失败={}, 耗时={}ms",
                    allGroupIds.size(), syncCount, errorCount, (endTime - startTime));
        } catch (Exception e) {
            log.error("同步拼团数据到MongoDB任务异常", e);
        }
    }

    /**
     * 同步单个拼团数据到MongoDB
     */
    private void syncGroupToMongo(String groupId) {
        String metaKey = groupRedisKeyBuilder.buildGroupMetaKey(groupId);
        Map<Object, Object> meta = stringRedisTemplate.opsForHash().entries(metaKey);
        if (meta.isEmpty()) {
            return;
        }

        // 查询MongoDB中的拼团实例
        GroupInstance instance = mongoTemplate.findOne(
                GroupInstance.buildIdQuery(groupId), GroupInstance.class);

        if (instance == null) {
            // 如果MongoDB中不存在，记录日志，后续可以通过其他方式同步
            log.warn("拼团实例在MongoDB中不存在，需要完整同步: groupId={}", groupId);
        } else {
            // 更新状态
            String status = String.valueOf(meta.get("status"));
            if (!status.equals(instance.getStatus())) {
                Query query = GroupInstance.buildIdQuery(groupId);
                mongoTemplate.updateFirst(query,
                        GroupInstance.buildStatusUpdate(status),
                        GroupInstance.class);
                log.debug("同步拼团状态到MongoDB: groupId={}, status={}", groupId, status);
            }
        }
    }

    /**
     * 活动状态自动更新任务
     * 每5分钟执行一次
     */
    @XxlJob("activityStatusUpdateJobHandler")
    public void activityStatusUpdateJobHandler() {
        log.info("开始更新活动状态");
        long startTime = System.currentTimeMillis();
        int updateCount = 0;

        try {
            Date now = new Date();

            // 更新未开始->进行中
            Query startQuery = GroupActivity.buildStatusQuery(0);
            startQuery.addCriteria(
                    Criteria.where("startTime").lte(now)
                            .and("endTime").gte(now)
            );
            long started = mongoTemplate.updateMulti(startQuery,
                    GroupActivity.buildStatusUpdate(1),
                    GroupActivity.class).getModifiedCount();

            // 更新进行中->已结束
            Query endQuery = GroupActivity.buildStatusQuery(1);
            endQuery.addCriteria(
                    Criteria.where("endTime").lt(now)
            );
            long ended = mongoTemplate.updateMulti(endQuery,
                    GroupActivity.buildStatusUpdate(2),
                    GroupActivity.class).getModifiedCount();

            updateCount = (int) (started + ended);

            long endTime = System.currentTimeMillis();
            log.info("活动状态更新完成: 开始={}, 结束={}, 总计={}, 耗时={}ms",
                    started, ended, updateCount, (endTime - startTime));
        } catch (Exception e) {
            log.error("活动状态更新任务异常", e);
        }
    }

}
