package com.ww.mall.promotion.service.group;

import com.ww.app.common.common.AppPage;
import com.ww.app.common.common.AppPageResult;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;

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
     * 分页查询用户参与的拼团列表
     */
    AppPageResult<GroupInstanceVO> getUserGroups(AppPage page);

    /**
     * 分页查询活动下的拼团列表
     */
    AppPageResult<GroupInstanceVO> getActivityGroups(String activityId, String status, AppPage page);
}
