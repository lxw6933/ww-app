package com.ww.mall.promotion.service.group.impl;

import com.ww.app.common.exception.ApiException;
import com.ww.app.redis.annotation.RedisPublishMsg;
import com.ww.mall.promotion.constants.RedisChannelConstant;
import com.ww.mall.promotion.controller.admin.group.req.GroupActivityBO;
import com.ww.mall.promotion.service.group.convert.GroupConvert;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.key.GroupRedisKeyBuilder;
import com.ww.mall.promotion.service.group.GroupActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2025-12-08 17:40
 * @description: 拼团活动服务实现
 */
@Slf4j
@Service
public class GroupActivityServiceImpl implements GroupActivityService {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private GroupRedisKeyBuilder groupRedisKeyBuilder;

    @Override
    public GroupActivity createActivity(GroupActivityBO bo) {
        // 参数校验
        if (bo == null) {
            throw new ApiException("活动信息不能为空");
        }
        validateActivityBO(bo);

        // 构建活动实体
        GroupActivity activity = GroupConvert.INSTANCE.groupActivityBOToActivity(bo);
        activity.setStatus(0); // 未开始
        activity.setSoldCount(0);
        activity.setEnabled(1); // 默认启用

        // 保存到MongoDB
        activity = mongoTemplate.save(activity);

        // 初始化Redis库存
        String stockKey = groupRedisKeyBuilder.buildActivityStockKey(activity.getId());
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(bo.getTotalStock()));

        log.info("创建拼团活动成功: activityId={}, name={}", activity.getId(), activity.getName());
        return activity;
    }

    @Override
    @RedisPublishMsg(value = RedisChannelConstant.GROUP_ACTIVITY_CACHE_CHANNEL, message = "#bo.id")
    public GroupActivity updateActivity(GroupActivityBO bo) {
        if (bo == null || bo.getId() == null || bo.getId().trim().isEmpty()) {
            throw new ApiException("活动ID不能为空");
        }
        GroupActivity activity = getActivityById(bo.getId());

        // 如果活动已开始，不允许修改关键信息
        Date now = new Date();
        if (activity.getStartTime().before(now) && activity.getEndTime().after(now)) {
            throw new ApiException("活动进行中，不允许修改");
        }

        // 更新活动信息
        GroupConvert.INSTANCE.updateGroupActivity(bo, activity);
        activity = mongoTemplate.save(activity);

        log.info("更新拼团活动成功: activityId={}", bo.getId());
        return activity;
    }

    @Override
    public GroupActivity getActivityById(String activityId) {
        if (activityId == null || activityId.trim().isEmpty()) {
            throw new ApiException("活动ID不能为空");
        }
        GroupActivity activity = mongoTemplate.findOne(GroupActivity.buildIdQuery(activityId), GroupActivity.class);
        if (activity == null) {
            throw new ApiException("活动不存在");
        }
        return activity;
    }

    @Override
    public List<GroupActivity> listActiveActivities() {
        Date now = new Date();
        Query query = GroupActivity.buildActiveQuery(1, now);
        return mongoTemplate.find(query, GroupActivity.class);
    }

    @Override
    public List<GroupActivity> getActivitiesBySpuId(Long spuId) {
        Query query = GroupActivity.buildSpuIdAndStatusQuery(spuId, 1);
        return mongoTemplate.find(query, GroupActivity.class);
    }

    @Override
    @RedisPublishMsg(value = RedisChannelConstant.GROUP_ACTIVITY_CACHE_CHANNEL, message = "#activityId")
    public void enableActivity(String activityId, Integer enabled) {
        if (activityId == null || activityId.trim().isEmpty()) {
            throw new ApiException("活动ID不能为空");
        }
        if (enabled == null || (enabled != 0 && enabled != 1)) {
            throw new ApiException("启用状态参数错误，只能是0或1");
        }
        mongoTemplate.updateFirst(GroupActivity.buildIdQuery(activityId), GroupActivity.buildEnabledUpdate(enabled), GroupActivity.class);
        log.info("{}活动: activityId={}", enabled == 1 ? "启用" : "禁用", activityId);
    }

    /**
     * 校验活动DTO
     */
    private void validateActivityBO(GroupActivityBO bo) {
        if (bo.getStartTime().after(bo.getEndTime())) {
            throw new ApiException("活动开始时间不能晚于结束时间");
        }
        if (bo.getRequiredSize() <= 1) {
            throw new ApiException("拼团人数必须大于1");
        }
        if (bo.getExpireHours() <= 0) {
            throw new ApiException("拼团有效期必须大于0");
        }
    }

}
