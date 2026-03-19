package com.ww.mall.promotion.service.group.impl;

import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.common.exception.ApiException;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.engine.GroupCommandService;
import com.ww.mall.promotion.engine.GroupQueryService;
import com.ww.mall.promotion.mq.GroupAfterSaleSuccessMessage;
import com.ww.mall.promotion.service.group.GroupInstanceService;
import com.ww.mall.promotion.service.group.command.CreateGroupCommand;
import com.ww.mall.promotion.service.group.command.JoinGroupCommand;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_ERROR;

/**
 * 拼团实例服务实现。
 * <p>
 * 重构后该类只保留应用层门面职责：
 * 命令走 Redis Lua 引擎，查询走 Redis 主状态与 Mongo 投影。
 *
 * @author ww
 * @create 2026-03-19
 * @description: 拼团实例服务实现
 */
@Service
public class GroupInstanceServiceImpl implements GroupInstanceService {

    @Resource
    private GroupCommandService groupCommandService;

    @Resource
    private GroupQueryService groupQueryService;

    @Override
    public GroupInstanceVO createGroup(CreateGroupCommand command) {
        return groupCommandService.createGroup(command);
    }

    @Override
    public GroupInstanceVO joinGroup(JoinGroupCommand command) {
        return groupCommandService.joinGroup(command);
    }

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

    @Override
    public void handleGroupSuccess(String groupId) {
        groupQueryService.getGroupDetail(groupId);
    }

    @Override
    public void handleGroupFailed(String groupId) {
        groupCommandService.expireGroup(groupId, "拼团过期未成团");
    }

    @Override
    public void handleAfterSaleSuccess(GroupAfterSaleSuccessMessage message) {
        groupCommandService.handleAfterSaleSuccess(message);
    }
}
