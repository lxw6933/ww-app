package com.ww.mall.promotion.service.group.impl;

import com.ww.app.common.exception.ApiException;
import com.ww.mall.promotion.controller.admin.group.res.GroupAdminDetailVO;
import com.ww.mall.promotion.engine.GroupStorageComponent;
import com.ww.mall.promotion.entity.group.GroupInstance;
import com.ww.mall.promotion.entity.group.GroupMember;
import com.ww.mall.promotion.service.group.GroupAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_NOT_EXISTS;

/**
 * 拼团后台查询服务实现。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 直接聚合团实例和成员轨迹，避免后台页面二次拼装
 */
@Slf4j
@Service
public class GroupAdminServiceImpl implements GroupAdminService {

    @Resource
    private GroupStorageComponent groupStorageComponent;

    @Override
    public GroupAdminDetailVO getDetail(String groupId) {
        GroupInstance instance = groupStorageComponent.findMongoGroupInstance(groupId);
        if (instance == null) {
            throw new ApiException(GROUP_RECORD_NOT_EXISTS);
        }
        List<GroupMember> members = groupStorageComponent.findMongoGroupMembers(groupId);
        GroupAdminDetailVO detail = new GroupAdminDetailVO();
        detail.setGroupId(instance.getId());
        detail.setActivityId(instance.getActivityId());
        detail.setStatus(instance.getStatus());
        detail.setSpuId(instance.getSpuId());
        detail.setLeaderUserId(instance.getLeaderUserId());
        detail.setCurrentSize(instance.getCurrentSize());
        detail.setRequiredSize(instance.getRequiredSize());
        detail.setRemainingSlots(instance.getRemainingSlots());
        detail.setFailReason(instance.getFailReason());
        detail.setExpireTime(instance.getExpireTime());
        detail.setCompleteTime(instance.getCompleteTime());
        detail.setMembers(members.stream().map(this::convertMember).collect(Collectors.toList()));
        return detail;
    }

    /**
     * 转换成员轨迹。
     */
    private GroupAdminDetailVO.MemberTrajectoryVO convertMember(GroupMember member) {
        GroupAdminDetailVO.MemberTrajectoryVO trajectoryVO = new GroupAdminDetailVO.MemberTrajectoryVO();
        trajectoryVO.setUserId(member.getUserId());
        trajectoryVO.setOrderId(member.getOrderId());
        trajectoryVO.setSkuId(member.getSkuId());
        trajectoryVO.setLeader(member.getIsLeader() != null && member.getIsLeader() == 1);
        trajectoryVO.setMemberStatus(member.getMemberStatus());
        trajectoryVO.setJoinTime(member.getJoinTime());
        trajectoryVO.setLatestTrajectory(member.getLatestTrajectory());
        trajectoryVO.setLatestTrajectoryTime(member.getLatestTrajectoryTime());
        return trajectoryVO;
    }
}
