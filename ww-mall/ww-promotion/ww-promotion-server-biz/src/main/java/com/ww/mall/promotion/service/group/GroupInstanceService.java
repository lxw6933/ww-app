package com.ww.mall.promotion.service.group;

import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;

import java.util.List;

/**
 * @author ww
 * @create 2025-12-08 17:45
 * @description: 拼团实例服务接口
 */
public interface GroupInstanceService {

    /**
     * 查询拼团详情
     */
    GroupInstanceVO getGroupDetail(String groupId);

    /**
     * 查询用户参与的拼团列表
     */
    List<GroupInstanceVO> getUserGroups();

    /**
     * 查询活动下的拼团列表
     */
    List<GroupInstanceVO> getActivityGroups(String activityId, String status);
}
