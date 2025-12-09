package com.ww.mall.promotion.service.group;

import com.ww.mall.promotion.controller.admin.group.req.GroupActivityBO;
import com.ww.mall.promotion.entity.group.GroupActivity;

import java.util.List;

/**
 * @author ww
 * @create 2025-12-08 17:40
 * @description: 拼团活动服务接口
 */
public interface GroupActivityService {

    /**
     * 创建拼团活动
     */
    GroupActivity createActivity(GroupActivityBO dto);

    /**
     * 更新拼团活动
     */
    GroupActivity updateActivity(GroupActivityBO dto);

    /**
     * 根据ID查询活动
     */
    GroupActivity getActivityById(String activityId);

    /**
     * 查询进行中的活动列表
     */
    List<GroupActivity> listActiveActivities();

    /**
     * 根据SPU ID查询活动
     */
    List<GroupActivity> getActivitiesBySpuId(Long spuId);

    /**
     * 启用/禁用活动
     */
    void enableActivity(String activityId, Integer enabled);

}

