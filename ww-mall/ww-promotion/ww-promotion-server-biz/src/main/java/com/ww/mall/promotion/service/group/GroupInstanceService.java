package com.ww.mall.promotion.service.group;

import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.mq.GroupAfterSaleSuccessMessage;
import com.ww.mall.promotion.service.group.command.CreateGroupCommand;
import com.ww.mall.promotion.service.group.command.JoinGroupCommand;

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
    GroupInstanceVO createGroup(CreateGroupCommand command);

    /**
     * 加入拼团
     */
    GroupInstanceVO joinGroup(JoinGroupCommand command);

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

    /**
     * 处理拼团完成
     */
    void handleGroupSuccess(String groupId);

    /**
     * 处理拼团失败
     */
    void handleGroupFailed(String groupId);

    /**
     * 处理售后成功并归还名额。
     *
     * @param message 售后成功消息
     */
    void handleAfterSaleSuccess(GroupAfterSaleSuccessMessage message);

}
