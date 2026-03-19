package com.ww.mall.promotion.engine;

import com.ww.app.common.exception.ApiException;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.engine.model.GroupCacheSnapshot;
import com.ww.mall.promotion.entity.group.GroupInstance;
import com.ww.mall.promotion.entity.group.GroupMember;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_ERROR;
import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_NOT_EXISTS;

/**
 * 拼团查询服务。
 *
 * @author ww
 * @create 2026-03-19
 * @description: 查询面优先读取 Redis 主状态，Mongo 仅作为降级与列表投影
 */
@Service
public class GroupQueryService {

    @Resource
    private GroupRedisStateReader groupRedisStateReader;

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 查询拼团详情。
     *
     * @param groupId 团ID
     * @return 详情
     */
    public GroupInstanceVO getGroupDetail(String groupId) {
        GroupCacheSnapshot snapshot = groupRedisStateReader.loadGroupSnapshot(groupId);
        if (snapshot != null) {
            return convertToVO(snapshot.getInstance(), snapshot.getMembers());
        }
        GroupInstance instance = mongoTemplate.findOne(GroupInstance.buildIdQuery(groupId), GroupInstance.class);
        if (instance == null) {
            throw new ApiException(GROUP_RECORD_NOT_EXISTS);
        }
        List<GroupMember> members = mongoTemplate.find(GroupMember.buildGroupInstanceIdQuery(groupId), GroupMember.class);
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
        List<String> groupIds = mongoTemplate.find(new Query().addCriteria(Criteria.where("userId").is(userId)),
                        GroupMember.class).stream()
                .map(GroupMember::getGroupInstanceId)
                .filter(this::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (groupIds == null || groupIds.isEmpty()) {
            return Collections.emptyList();
        }
        return loadGroupDetails(groupIds);
    }

    /**
     * 查询活动下的拼团列表。
     *
     * @param activityId 活动ID
     * @param status 状态
     * @return 拼团列表
     */
    public List<GroupInstanceVO> getActivityGroups(String activityId, String status) {
        if (!hasText(activityId)) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        return mongoTemplate.find(GroupInstance.buildActivityIdAndStatusQuery(activityId, status), GroupInstance.class)
                .stream()
                .map(instance -> convertSummaryToVO(instance))
                .collect(Collectors.toList());
    }

    /**
     * 批量加载详情。
     *
     * @param groupIds 团ID集合
     * @return 详情列表
     */
    private List<GroupInstanceVO> loadGroupDetails(Collection<String> groupIds) {
        List<GroupInstanceVO> result = new ArrayList<>();
        for (String groupId : groupIds) {
            if (!hasText(groupId)) {
                continue;
            }
            try {
                result.add(getGroupDetail(groupId));
            } catch (Exception ignore) {
            }
        }
        return result;
    }

    /**
     * 将完整实体转换为返回视图。
     *
     * @param instance 团实例
     * @param members 成员
     * @return 视图对象
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
     * 将摘要实体转换为返回视图。
     *
     * @param instance 团实例
     * @return 视图对象
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
