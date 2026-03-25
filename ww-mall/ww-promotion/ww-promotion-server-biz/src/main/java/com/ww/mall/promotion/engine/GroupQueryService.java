package com.ww.mall.promotion.engine;

import com.ww.app.common.exception.ApiException;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.engine.model.GroupCacheSnapshot;
import com.ww.mall.promotion.entity.group.GroupInstance;
import com.ww.mall.promotion.entity.group.GroupMember;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_ERROR;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_NOT_EXISTS;

/**
 * 拼团查询服务。
 * <p>
 * 查询链路优先读取 Redis 主状态，Mongo 仅作为详情降级与列表摘要存储。
 * 具体 Redis/Mongo 访问能力统一通过 {@link GroupStorageComponent} 下沉。
 *
 * @author ww
 * @create 2026-03-19
 * @description: 拼团查询服务
 */
@Service
public class GroupQueryService {

    @Resource
    private GroupStorageComponent groupStorageComponent;

    /**
     * 查询拼团详情。
     *
     * @param groupId 团ID
     * @return 拼团详情
     */
    public GroupInstanceVO getGroupDetail(String groupId) {
        GroupCacheSnapshot snapshot = groupStorageComponent.loadGroupSnapshot(groupId);
        if (snapshot != null) {
            return convertToVO(snapshot.getInstance(), snapshot.getMembers());
        }
        GroupInstance instance = groupStorageComponent.findMongoGroupInstance(groupId);
        if (instance == null) {
            throw new ApiException(GROUP_RECORD_NOT_EXISTS);
        }
        List<GroupMember> members = groupStorageComponent.findMongoGroupMembers(groupId);
        return convertToVO(instance, members);
    }

    /**
     * 查询用户参与的拼团列表。
     *
     * @param userId 用户ID
     * @return 拼团列表
     */
    public List<GroupInstanceVO> getUserGroups(Long userId) {
        if (userId == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        List<GroupMember> userMembers = groupStorageComponent.findMongoUserMembers(userId);
        if (userMembers == null || userMembers.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> orderedGroupIds = new LinkedHashSet<>();
        userMembers.forEach(member -> {
            if (member != null && hasText(member.getGroupInstanceId())) {
                orderedGroupIds.add(member.getGroupInstanceId());
            }
        });
        if (orderedGroupIds.isEmpty()) {
            return Collections.emptyList();
        }
        return loadGroupSummaries(new ArrayList<>(orderedGroupIds));
    }

    /**
     * 查询活动下的拼团列表。
     *
     * @param activityId 活动ID
     * @param status 团状态
     * @return 拼团列表
     */
    public List<GroupInstanceVO> getActivityGroups(String activityId, String status) {
        if (!hasText(activityId)) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        return groupStorageComponent.findMongoActivityGroupSummaries(activityId, status)
                .stream()
                .map(this::convertSummaryToVO)
                .collect(Collectors.toList());
    }

    /**
     * 批量加载团摘要。
     *
     * @param groupIds 团ID集合
     * @return 团摘要列表
     */
    private List<GroupInstanceVO> loadGroupSummaries(Collection<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<GroupInstance> instances = groupStorageComponent.findMongoGroupSummaries(new ArrayList<>(groupIds));
        if (instances == null || instances.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, GroupInstance> instanceMap = new LinkedHashMap<>();
        instances.forEach(instance -> {
            if (instance != null && hasText(instance.getId())) {
                instanceMap.put(instance.getId(), instance);
            }
        });
        List<GroupInstanceVO> result = new ArrayList<>();
        for (String groupId : groupIds) {
            if (!hasText(groupId)) {
                continue;
            }
            GroupInstance instance = instanceMap.get(groupId);
            if (instance != null) {
                result.add(convertSummaryToVO(instance));
            }
        }
        return result;
    }

    /**
     * 将完整实体转换为返回视图。
     *
     * @param instance 团实体
     * @param members 成员列表
     * @return 返回视图
     */
    private GroupInstanceVO convertToVO(GroupInstance instance, List<GroupMember> members) {
        GroupInstanceVO vo = convertSummaryToVO(instance);
        List<GroupInstanceVO.MemberInfo> memberInfos = new ArrayList<>();
        for (GroupMember member : members) {
            GroupInstanceVO.MemberInfo memberInfo = new GroupInstanceVO.MemberInfo();
            memberInfo.setUserId(member.getUserId());
            memberInfo.setOrderId(member.getOrderId());
            memberInfo.setSkuId(member.getSkuId());
            memberInfo.setJoinTime(member.getJoinTime());
            memberInfo.setIsLeader(member.getIsLeader() != null && member.getIsLeader() == 1);
            memberInfo.setMemberStatus(member.getMemberStatus());
            memberInfo.setLatestTrajectory(member.getLatestTrajectory());
            memberInfo.setLatestTrajectoryTime(member.getLatestTrajectoryTime());
            memberInfos.add(memberInfo);
        }
        vo.setMembers(memberInfos);
        return vo;
    }

    /**
     * 将团摘要实体转换为返回视图。
     *
     * @param instance 团实体
     * @return 返回视图
     */
    private GroupInstanceVO convertSummaryToVO(GroupInstance instance) {
        GroupInstanceVO vo = new GroupInstanceVO();
        vo.setId(instance.getId());
        vo.setActivityId(instance.getActivityId());
        vo.setLeaderUserId(instance.getLeaderUserId());
        vo.setStatus(instance.getStatus());
        vo.setRequiredSize(instance.getRequiredSize());
        vo.setCurrentSize(instance.getCurrentSize());
        vo.setRemainingSlots(instance.getRemainingSlots());
        vo.setExpireTime(instance.getExpireTime());
        vo.setCompleteTime(instance.getCompleteTime());
        vo.setSpuId(instance.getSpuId());
        vo.setSkuIds(instance.getSkuIds());
        vo.setFailReason(instance.getFailReason());
        List<GroupInstanceVO.MemberInfo> memberInfos = new ArrayList<>();
        if (instance.getMembers() != null) {
            instance.getMembers().forEach(member -> {
                GroupInstanceVO.MemberInfo memberInfo = new GroupInstanceVO.MemberInfo();
                memberInfo.setUserId(member.getUserId());
                memberInfo.setOrderId(member.getOrderId());
                memberInfo.setSkuId(member.getSkuId());
                memberInfo.setJoinTime(member.getJoinTime());
                memberInfo.setIsLeader(Boolean.TRUE.equals(member.getIsLeader()));
                memberInfo.setMemberStatus(member.getMemberStatus());
                memberInfo.setLatestTrajectory(member.getLatestTrajectory());
                memberInfo.setLatestTrajectoryTime(member.getLatestTrajectoryTime());
                memberInfos.add(memberInfo);
            });
        }
        vo.setMembers(memberInfos);
        return vo;
    }

    /**
     * 判断文本是否有值。
     *
     * @param value 文本
     * @return true-有值
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
