package com.ww.mall.promotion.service.group.impl;

import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.common.exception.ApiException;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.engine.GroupQueryService;
import com.ww.mall.promotion.service.group.GroupInstanceService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_ERROR;

/**
 * 拼团实例服务实现。
 * <p>
 * 当前该类仅保留查询门面职责：
 * 1. C 端详情查询。
 * 2. 当前用户参与团列表查询。
 * 3. 活动下团列表查询。
 *
 * @author ww
 * @create 2026-03-19
 * @description: 拼团实例查询门面服务实现
 */
@Service
public class GroupInstanceServiceImpl implements GroupInstanceService {

    @Resource
    private GroupQueryService groupQueryService;

    @Override
    public GroupInstanceVO getGroupDetail(String groupId) {
        return groupQueryService.getGroupDetail(groupId);
    }

    @Override
    public List<GroupInstanceVO> getUserGroups() {
        Long userId = AuthorizationContext.getClientUser() != null ? AuthorizationContext.getClientUser().getId() : null;
        if (userId == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        return groupQueryService.getUserGroups(userId);
    }

    @Override
    public List<GroupInstanceVO> getActivityGroups(String activityId, String status) {
        return groupQueryService.getActivityGroups(activityId, status);
    }
}
