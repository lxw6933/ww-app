package com.ww.mall.promotion.engine;

import com.ww.app.common.common.AppPage;
import com.ww.app.common.common.AppPageResult;
import com.ww.app.common.exception.ApiException;
import com.ww.mall.promotion.component.GroupStorageComponent;
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
import java.util.List;
import java.util.Map;
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
     * 分页查询用户参与的拼团列表。
     *
     * @param userId 用户ID
     * @param page 分页参数
     * @return 分页结果
     */
    public AppPageResult<GroupInstanceVO> getUserGroups(Long userId, AppPage page) {
        if (userId == null) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        AppPage safePage = page == null ? new AppPage() : page;
        long totalCount = groupStorageComponent.countMongoUserGroups(userId);
        if (totalCount <= 0L) {
            return new AppPageResult<>(safePage, Collections.emptyList(), 0);
        }
        List<String> groupIds = groupStorageComponent.findMongoUserGroupIds(
                userId,
                safePage.getPageNum(),
                safePage.getPageSize()
        );
        List<GroupInstanceVO> result = groupIds.isEmpty()
                ? Collections.emptyList()
                : loadGroupSummaries(groupIds);
        return new AppPageResult<>(safePage, result, safeTotalCount(totalCount));
    }

    /**
     * 分页查询活动下的拼团列表。
     *
     * @param activityId 活动ID
     * @param status 团状态
     * @param page 分页参数
     * @return 分页结果，其中 totalCount 表示“当前筛选条件下的团记录数”；
     * 不等同于活动累计开团数或累计参团人数，这两类统计仍由 Redis 活动统计回填。
     */
    public AppPageResult<GroupInstanceVO> getActivityGroups(String activityId, String status, AppPage page) {
        if (!hasText(activityId)) {
            throw new ApiException(GROUP_RECORD_ERROR);
        }
        AppPage safePage = page == null ? new AppPage() : page;
        long totalCount = groupStorageComponent.countMongoActivityGroups(activityId, status);
        if (totalCount <= 0L) {
            return new AppPageResult<>(safePage, Collections.emptyList(), 0);
        }
        List<GroupInstanceVO> result = groupStorageComponent.findMongoActivityGroupSummaries(
                        activityId,
                        status,
                        safePage.getPageNum(),
                        safePage.getPageSize()
                ).stream()
                .map(this::convertSummaryToVO)
                .collect(Collectors.toList());
        return new AppPageResult<>(safePage, result, safeTotalCount(totalCount));
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
            memberInfo.setMemberStatus(member.getMemberStatus());
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
                memberInfo.setMemberStatus(member.getMemberStatus());
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

    /**
     * 将长整型总数安全收敛为分页对象使用的整型。
     *
     * @param totalCount 原始总数
     * @return 安全总数
     */
    private int safeTotalCount(long totalCount) {
        return totalCount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalCount;
    }
}
