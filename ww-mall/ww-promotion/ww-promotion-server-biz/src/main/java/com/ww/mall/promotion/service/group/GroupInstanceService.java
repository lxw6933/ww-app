package com.ww.mall.promotion.service.group;

import com.ww.mall.promotion.controller.app.group.req.CreateGroupRequest;
import com.ww.mall.promotion.controller.app.group.req.JoinGroupRequest;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;

import java.util.List;

/**
 * @author ww
 * @create 2025-12-08 17:45
 * @description: 拼团实例服务接口
 */
public interface GroupInstanceService {

    /**
     * 创建拼团
     */
    GroupInstanceVO createGroup(CreateGroupRequest request);

    /**
     * 加入拼团
     */
    GroupInstanceVO joinGroup(JoinGroupRequest request);

    /**
     * 查询拼团详情
     */
    GroupInstanceVO getGroupDetail(String groupId);

    /**
     * 查询用户参与的拼团列表
     */
    List<GroupInstanceVO> getUserGroups(Long userId);

    /**
     * 查询活动下的拼团列表
     */
    List<GroupInstanceVO> getActivityGroups(String activityId, String status);

    /**
     * 处理拼团完成
     */
    void handleGroupSuccess(String groupId);

    /**
     * 处理拼团失败
     */
    void handleGroupFailed(String groupId);

}

