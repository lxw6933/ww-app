package com.ww.mall.promotion.service.group.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.ww.app.common.exception.ApiException;
import com.ww.app.redis.annotation.RedisPublishMsg;
import com.ww.mall.promotion.component.GroupStorageComponent;
import com.ww.mall.promotion.constants.RedisChannelConstant;
import com.ww.mall.promotion.controller.admin.group.req.GroupActivityBO;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.enums.GroupEnabledStatus;
import com.ww.mall.promotion.service.group.GroupActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_ACTIVITY_ENABLED_INVALID;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_ACTIVITY_EXPIRE_HOURS_INVALID;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_ACTIVITY_ID_EMPTY;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_ACTIVITY_NOT_EXISTS;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_ACTIVITY_PARAM_EMPTY;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_ACTIVITY_REQUIRED_SIZE_INVALID;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_ACTIVITY_SKU_RULE_REQUIRED;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_ACTIVITY_START_END_INVALID;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_ACTIVITY_UPDATING_FORBIDDEN;

/**
 * 拼团活动服务实现。
 * <p>
 * 该实现不再维护持久化 status 字段，而是统一以开始时间和结束时间为准，
 * 在查询和返回阶段动态推导活动状态。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 以活动维度统一管理多个 SPU，每个 SPU 下再维护自己的 SKU 拼团规则
 */
@Slf4j
@Service
public class GroupActivityServiceImpl implements GroupActivityService {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private GroupStorageComponent groupStorageComponent;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public GroupActivity createActivity(GroupActivityBO bo) {
        if (bo == null) {
            throw new ApiException(GROUP_ACTIVITY_PARAM_EMPTY);
        }
        validateActivityBO(bo);

        GroupActivity activity = buildActivity(bo, new GroupActivity());
        Date now = new Date();
        activity.setCreateTime(now);
        activity.setUpdateTime(now);
        activity.setEnabled(GroupEnabledStatus.ENABLED.isEnabled());
        activity.setOpenGroupCount(0L);
        activity.setJoinMemberCount(0L);
        activity.setStatsSettled(false);
        GroupActivity saved = mongoTemplate.save(activity);
        groupStorageComponent.fillActivityStatistics(saved);
        log.info("创建拼团活动成功: activityId={}, spuConfigSize={}, skuRuleSize={}",
                saved.getId(), size(saved.getSpuConfigs()), countSkuRules(saved.getSpuConfigs()));
        return saved;
    }

    @Override
    @RedisPublishMsg(value = RedisChannelConstant.GROUP_ACTIVITY_CACHE_CHANNEL, message = "#bo.id")
    public GroupActivity updateActivity(GroupActivityBO bo) {
        if (bo == null || bo.getId() == null || bo.getId().trim().isEmpty()) {
            throw new ApiException(GROUP_ACTIVITY_ID_EMPTY);
        }
        validateActivityBO(bo);
        GroupActivity activity = getActivityById(bo.getId());
        Date now = new Date();
        if (activity.isActiveAt(now)) {
            throw new ApiException(GROUP_ACTIVITY_UPDATING_FORBIDDEN);
        }
        GroupActivity updated = buildActivity(bo, activity);
        updated.setUpdateTime(now);
        GroupActivity saved = mongoTemplate.save(updated);
        groupStorageComponent.fillActivityStatistics(saved);
        log.info("更新拼团活动成功: activityId={}, spuConfigSize={}, skuRuleSize={}",
                saved.getId(), size(saved.getSpuConfigs()), countSkuRules(saved.getSpuConfigs()));
        return saved;
    }

    @Override
    public GroupActivity getActivityById(String activityId) {
        if (activityId == null || activityId.trim().isEmpty()) {
            throw new ApiException(GROUP_ACTIVITY_ID_EMPTY);
        }
        GroupActivity activity = mongoTemplate.findOne(GroupActivity.buildIdQuery(activityId), GroupActivity.class);
        if (activity == null) {
            throw new ApiException(GROUP_ACTIVITY_NOT_EXISTS);
        }
        groupStorageComponent.fillActivityStatistics(activity);
        return activity;
    }

    @Override
    public List<GroupActivity> listActiveActivities() {
        Date now = new Date();
        Query query = GroupActivity.buildActiveQuery(now);
        List<GroupActivity> activities = mongoTemplate.find(query, GroupActivity.class);
        groupStorageComponent.fillActivityStatistics(activities);
        return activities;
    }

    @Override
    public List<GroupActivity> getActivitiesBySpuId(Long spuId) {
        Date now = new Date();
        Query query = GroupActivity.buildSpuIdAndActiveQuery(spuId, now);
        List<GroupActivity> activities = mongoTemplate.find(query, GroupActivity.class);
        groupStorageComponent.fillActivityStatistics(activities);
        activities.forEach(activity -> retainOnlyMatchedSpuConfig(activity, spuId));
        return activities;
    }

    @Override
    @RedisPublishMsg(value = RedisChannelConstant.GROUP_ACTIVITY_CACHE_CHANNEL, message = "#activityId")
    public void enableActivity(String activityId, GroupEnabledStatus enabledStatus) {
        if (activityId == null || activityId.trim().isEmpty()) {
            throw new ApiException(GROUP_ACTIVITY_ID_EMPTY);
        }
        if (enabledStatus == null) {
            throw new ApiException(GROUP_ACTIVITY_ENABLED_INVALID);
        }
        mongoTemplate.updateFirst(GroupActivity.buildIdQuery(activityId),
                GroupActivity.buildEnabledUpdate(enabledStatus.isEnabled()), GroupActivity.class);
        log.info("{}活动: activityId={}",
                GroupEnabledStatus.ENABLED == enabledStatus ? "启用" : "禁用", activityId);
    }

    @Override
    public int settleExpiredActivityStatistics() {
        Date now = new Date();
        Query query = GroupActivity.buildEndedUnsettledQuery(now, com.ww.mall.promotion.constants.GroupBizConstants.ACTIVITY_STATS_SETTLE_BATCH_LIMIT);
        List<GroupActivity> activities = mongoTemplate.find(query, GroupActivity.class);
        if (CollectionUtil.isEmpty(activities)) {
            log.debug("活动统计归档跳过: 无待归档活动");
            return 0;
        }
        int settledCount = 0;
        for (GroupActivity activity : activities) {
            if (settleSingleActivityStatistics(activity, now)) {
                settledCount++;
            }
        }
        log.info("活动统计归档完成: scannedCount={}, settledCount={}", activities.size(), settledCount);
        return settledCount;
    }

    /**
     * 构建活动实体。
     * <p>
     * 该方法负责把请求对象转换为持久化对象。
     * 当前模型允许一个活动挂多个 SPU，每个 SPU 再承载自己的 SKU 拼团规则。
     *
     * @param bo 请求对象
     * @param target 目标实体
     * @return 活动实体
     */
    private GroupActivity buildActivity(GroupActivityBO bo, GroupActivity target) {
        target.setName(bo.getName());
        target.setDescription(bo.getDescription());
        target.setRequiredSize(bo.getRequiredSize());
        target.setExpireHours(bo.getExpireHours());
        target.setStartTime(bo.getStartTime());
        target.setEndTime(bo.getEndTime());
        target.setLimitPerUser(bo.getLimitPerUser());
        target.setSpuConfigs(normalizeSpuConfigs(bo));
        return target;
    }

    /**
     * 归一化 SPU 配置。
     * <p>
     * 该方法会忽略不完整的配置项，仅保留“SPU ID 完整且至少存在一条有效 SKU 规则”的配置。
     *
     * @param bo 活动请求对象
     * @return SPU 配置列表
     */
    private List<GroupActivity.GroupSpuConfig> normalizeSpuConfigs(GroupActivityBO bo) {
        List<GroupActivity.GroupSpuConfig> spuConfigs = new ArrayList<>();
        if (CollectionUtil.isEmpty(bo.getSpuConfigs())) {
            throw new ApiException(GROUP_ACTIVITY_SKU_RULE_REQUIRED);
        }
        for (GroupActivityBO.GroupSpuConfigBO spuConfigBO : bo.getSpuConfigs()) {
            if (spuConfigBO == null || spuConfigBO.getSpuId() == null || CollectionUtil.isEmpty(spuConfigBO.getSkuRules())) {
                continue;
            }
            GroupActivity.GroupSpuConfig spuConfig = new GroupActivity.GroupSpuConfig();
            spuConfig.setSpuId(spuConfigBO.getSpuId());
            List<GroupActivity.GroupSkuRule> skuRules = new ArrayList<>();
            for (GroupActivityBO.GroupSkuRuleBO skuRuleBO : spuConfigBO.getSkuRules()) {
                if (skuRuleBO == null || skuRuleBO.getSkuId() == null || skuRuleBO.getGroupPrice() == null) {
                    continue;
                }
                GroupActivity.GroupSkuRule rule = new GroupActivity.GroupSkuRule();
                rule.setSkuId(skuRuleBO.getSkuId());
                rule.setGroupPrice(skuRuleBO.getGroupPrice());
                rule.setEnabled(skuRuleBO.getEnabled() == null ? Boolean.TRUE : skuRuleBO.getEnabled());
                skuRules.add(rule);
            }
            if (CollectionUtil.isEmpty(skuRules)) {
                continue;
            }
            spuConfig.setSkuRules(skuRules);
            spuConfigs.add(spuConfig);
        }
        if (CollectionUtil.isEmpty(spuConfigs)) {
            throw new ApiException(GROUP_ACTIVITY_SKU_RULE_REQUIRED);
        }
        return spuConfigs;
    }

    /**
     * 校验活动请求。
     * <p>
     * 重点校验以下边界：
     * 1. 时间窗必须合法。
     * 2. 成团人数和有效期必须为正数。
     * 3. 至少存在一个 SPU 配置，且 SKU 规则至少有一条。
     * 4. 活动内 SPU ID、SKU ID 不允许重复，避免交易时命中歧义。
     *
     * @param bo 活动请求
     */
    private void validateActivityBO(GroupActivityBO bo) {
        if (bo.getStartTime() == null || bo.getEndTime() == null || bo.getStartTime().after(bo.getEndTime())) {
            throw new ApiException(GROUP_ACTIVITY_START_END_INVALID);
        }
        if (bo.getRequiredSize() == null || bo.getRequiredSize() <= 1) {
            throw new ApiException(GROUP_ACTIVITY_REQUIRED_SIZE_INVALID);
        }
        if (bo.getExpireHours() == null || bo.getExpireHours() <= 0) {
            throw new ApiException(GROUP_ACTIVITY_EXPIRE_HOURS_INVALID);
        }
        List<GroupActivity.GroupSpuConfig> normalizedSpuConfigs = normalizeSpuConfigs(bo);
        Set<Long> spuIds = new HashSet<>();
        Set<Long> skuIds = new HashSet<>();
        for (GroupActivity.GroupSpuConfig spuConfig : normalizedSpuConfigs) {
            if (!spuIds.add(spuConfig.getSpuId())) {
                throw new ApiException(GROUP_ACTIVITY_PARAM_EMPTY.getMsg() + ": duplicated spuId");
            }
            if (CollectionUtil.isEmpty(spuConfig.getSkuRules())) {
                throw new ApiException(GROUP_ACTIVITY_SKU_RULE_REQUIRED);
            }
            for (GroupActivity.GroupSkuRule skuRule : spuConfig.getSkuRules()) {
                if (skuRule.getGroupPrice() == null || skuRule.getGroupPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ApiException(GROUP_ACTIVITY_PARAM_EMPTY.getMsg() + ": groupPrice");
                }
                if (!skuIds.add(skuRule.getSkuId())) {
                    throw new ApiException(GROUP_ACTIVITY_PARAM_EMPTY.getMsg() + ": duplicated skuId");
                }
            }
        }
    }

    /**
     * 仅保留活动内命中的 SPU 配置。
     * <p>
     * 按 SPU 查询活动时，只返回当前 SPU 的配置切片，避免把同一活动下无关的 SPU 配置一并返回。
     *
     * @param activity 活动实体
     * @param spuId SPU ID
     */
    private void retainOnlyMatchedSpuConfig(GroupActivity activity, Long spuId) {
        if (activity == null || spuId == null || CollectionUtil.isEmpty(activity.getSpuConfigs())) {
            return;
        }
        activity.getSpuConfigs().stream()
                .filter(spuConfig -> spuConfig != null && spuId.equals(spuConfig.getSpuId()))
                .findFirst()
                .ifPresent(spuConfig -> activity.setSpuConfigs(Collections.singletonList(spuConfig)));
    }

    /**
     * 统计活动内 SKU 规则总数。
     *
     * @param spuConfigs SPU 配置列表
     * @return SKU 规则总数
     */
    private int countSkuRules(List<GroupActivity.GroupSpuConfig> spuConfigs) {
        if (CollectionUtil.isEmpty(spuConfigs)) {
            return 0;
        }
        return spuConfigs.stream()
                .filter(spuConfig -> spuConfig != null && !CollectionUtil.isEmpty(spuConfig.getSkuRules()))
                .mapToInt(spuConfig -> spuConfig.getSkuRules().size())
                .sum();
    }

    /**
     * 统计集合大小。
     *
     * @param values 集合
     * @return 大小
     */
    private int size(List<?> values) {
        return values == null ? 0 : values.size();
    }

    /**
     * 归档单个活动的最终统计数据。
     * <p>
     * 该方法先读取 Redis 中的最新累计值，再使用“仅未归档活动可更新”的条件更新 Mongo，
     * 确保多实例并发执行时只会有一个实例真正完成落库与删 Key。
     *
     * @param activity 活动实体
     * @param settledTime 归档时间
     * @return true-本次成功归档，false-被其他实例抢先归档或无需处理
     */
    private boolean settleSingleActivityStatistics(GroupActivity activity, Date settledTime) {
        if (activity == null || activity.getId() == null || activity.getId().trim().isEmpty()) {
            return false;
        }
        groupStorageComponent.fillActivityStatistics(activity);
        Query updateQuery = GroupActivity.buildIdAndUnsettledQuery(activity.getId());
        Update update = GroupActivity.buildStatisticsSettledUpdate(
                activity.getOpenGroupCount(),
                activity.getJoinMemberCount(),
                settledTime
        );
        long modifiedCount = mongoTemplate.updateFirst(updateQuery, update, GroupActivity.class).getModifiedCount();
        if (modifiedCount <= 0L) {
            log.debug("活动统计归档被跳过: activityId={}, modifiedCount={}", activity.getId(), modifiedCount);
            return false;
        }
        groupStorageComponent.clearActivityStatistics(activity.getId());
        stringRedisTemplate.convertAndSend(RedisChannelConstant.GROUP_ACTIVITY_CACHE_CHANNEL, activity.getId());
        log.info("活动统计归档成功: activityId={}, openGroupCount={}, joinMemberCount={}",
                activity.getId(), activity.getOpenGroupCount(), activity.getJoinMemberCount());
        return true;
    }
}
