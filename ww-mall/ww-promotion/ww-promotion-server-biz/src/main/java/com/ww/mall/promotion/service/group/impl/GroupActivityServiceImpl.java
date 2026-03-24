package com.ww.mall.promotion.service.group.impl;

import com.ww.app.common.exception.ApiException;
import com.ww.app.redis.annotation.RedisPublishMsg;
import com.ww.mall.promotion.constants.RedisChannelConstant;
import com.ww.mall.promotion.controller.admin.group.req.GroupActivityBO;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.enums.GroupEnabledStatus;
import com.ww.mall.promotion.service.group.GroupActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

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
 * @description: 以 SPU 维度定义拼团活动，使用 SKU 规则承载不同规格的成交价格
 */
@Slf4j
@Service
public class GroupActivityServiceImpl implements GroupActivityService {

    @Resource
    private MongoTemplate mongoTemplate;

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
        activity.setEnabled(GroupEnabledStatus.ENABLED.getCode());
        GroupActivity saved = mongoTemplate.save(activity);
        log.info("创建拼团活动成功: activityId={}, spuId={}, skuRuleSize={}",
                saved.getId(), saved.getSpuId(), saved.getSkuRules() != null ? saved.getSkuRules().size() : 0);
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
        log.info("更新拼团活动成功: activityId={}, spuId={}, skuRuleSize={}",
                saved.getId(), saved.getSpuId(), saved.getSkuRules() != null ? saved.getSkuRules().size() : 0);
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
        return activity;
    }

    @Override
    public List<GroupActivity> listActiveActivities() {
        Date now = new Date();
        Query query = GroupActivity.buildActiveQuery(now);
        return mongoTemplate.find(query, GroupActivity.class);
    }

    @Override
    public List<GroupActivity> getActivitiesBySpuId(Long spuId) {
        Date now = new Date();
        Query query = GroupActivity.buildSpuIdAndActiveQuery(spuId, now);
        return mongoTemplate.find(query, GroupActivity.class);
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
                GroupActivity.buildEnabledUpdate(enabledStatus.getCode()), GroupActivity.class);
        log.info("{}活动: activityId={}",
                GroupEnabledStatus.ENABLED == enabledStatus ? "启用" : "禁用", activityId);
    }

    /**
     * 构建活动实体。
     * <p>
     * 该方法负责把请求对象转换为持久化对象，并同步刷新兼容字段：
     * 1. skuRules 保存完整可售规则。
     * 2. skuId/groupPrice/originalPrice 保存当前用于展示的默认规则快照。
     *
     * @param bo 请求对象
     * @param target 目标实体
     * @return 活动实体
     */
    private GroupActivity buildActivity(GroupActivityBO bo, GroupActivity target) {
        target.setName(bo.getName());
        target.setDescription(bo.getDescription());
        target.setSpuId(bo.getSpuId());
        target.setRequiredSize(bo.getRequiredSize());
        target.setExpireHours(bo.getExpireHours());
        target.setStartTime(bo.getStartTime());
        target.setEndTime(bo.getEndTime());
        target.setLimitPerUser(bo.getLimitPerUser());
        target.setImageUrl(bo.getImageUrl());
        target.setSortWeight(bo.getSortWeight());

        List<GroupActivity.GroupSkuRule> skuRules = normalizeSkuRules(bo);
        target.setSkuRules(skuRules);
        GroupActivity.GroupSkuRule displayRule = resolveDisplayRule(skuRules);
        if (displayRule != null) {
            target.setSkuId(displayRule.getSkuId());
            target.setGroupPrice(displayRule.getGroupPrice());
            target.setOriginalPrice(displayRule.getOriginalPrice());
        } else {
            target.setSkuId(null);
            target.setGroupPrice(null);
            target.setOriginalPrice(null);
        }
        return target;
    }

    /**
     * 归一化 SKU 规则。
     * <p>
     * 该方法统一兼容新旧两种入参格式：
     * 1. 新格式优先使用 skuRules。
     * 2. 若未提供 skuRules，则回退到单 SKU 兼容字段。
     *
     * @param bo 活动请求对象
     * @return SKU 规则列表
     */
    private List<GroupActivity.GroupSkuRule> normalizeSkuRules(GroupActivityBO bo) {
        List<GroupActivity.GroupSkuRule> rules = new ArrayList<>();
        if (bo.getSkuRules() != null) {
            for (GroupActivityBO.GroupSkuRuleBO skuRuleBO : bo.getSkuRules()) {
                if (skuRuleBO == null || skuRuleBO.getSkuId() == null || skuRuleBO.getGroupPrice() == null) {
                    continue;
                }
                GroupActivity.GroupSkuRule rule = new GroupActivity.GroupSkuRule();
                rule.setSkuId(skuRuleBO.getSkuId());
                rule.setGroupPrice(skuRuleBO.getGroupPrice());
                rule.setOriginalPrice(skuRuleBO.getOriginalPrice());
                rule.setEnabled(skuRuleBO.getEnabled() == null ? GroupEnabledStatus.ENABLED.getCode() : skuRuleBO.getEnabled());
                rules.add(rule);
            }
        }
        if (rules.isEmpty() && bo.getSkuId() != null && bo.getGroupPrice() != null) {
            GroupActivity.GroupSkuRule legacyRule = new GroupActivity.GroupSkuRule();
            legacyRule.setSkuId(bo.getSkuId());
            legacyRule.setGroupPrice(bo.getGroupPrice());
            legacyRule.setOriginalPrice(bo.getOriginalPrice());
            legacyRule.setEnabled(GroupEnabledStatus.ENABLED.getCode());
            rules.add(legacyRule);
        }
        if (rules.isEmpty()) {
            throw new ApiException(GROUP_ACTIVITY_SKU_RULE_REQUIRED);
        }
        return rules;
    }

    /**
     * 选择默认展示规则。
     * <p>
     * 展示规则只从启用中的 SKU 规则里选择，避免页面展示一个实际已禁用的最低价 SKU。
     * 若当前所有规则均为禁用，则返回 null，让兼容展示字段清空，避免保留旧值造成误导。
     *
     * @param skuRules SKU 规则列表
     * @return 默认展示规则
     */
    private GroupActivity.GroupSkuRule resolveDisplayRule(List<GroupActivity.GroupSkuRule> skuRules) {
        if (skuRules == null || skuRules.isEmpty()) {
            return null;
        }
        return skuRules.stream()
                .filter(rule -> rule != null
                        && GroupEnabledStatus.DISABLED.getCode() != rule.getEnabled()
                        && rule.getGroupPrice() != null)
                .min(Comparator.comparing(GroupActivity.GroupSkuRule::getGroupPrice))
                .orElse(null);
    }

    /**
     * 校验活动请求。
     * <p>
     * 重点校验以下边界：
     * 1. 时间窗必须合法。
     * 2. 成团人数和有效期必须为正数。
     * 3. SKU 规则至少存在一条且价格必须大于 0。
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
        List<GroupActivity.GroupSkuRule> normalizedRules = normalizeSkuRules(bo);
        boolean invalidPrice = normalizedRules.stream()
                .map(GroupActivity.GroupSkuRule::getGroupPrice)
                .anyMatch(price -> price == null || price.compareTo(BigDecimal.ZERO) <= 0);
        if (invalidPrice) {
            throw new ApiException(GROUP_ACTIVITY_PARAM_EMPTY.getMsg() + ": groupPrice");
        }
    }
}
