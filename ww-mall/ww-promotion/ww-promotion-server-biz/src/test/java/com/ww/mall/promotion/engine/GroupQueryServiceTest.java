package com.ww.mall.promotion.engine;

import com.ww.mall.promotion.component.GroupStorageComponent;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.entity.group.GroupInstance;
import com.ww.mall.promotion.entity.group.GroupMember;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 拼团查询服务测试。
 *
 * @author ww
 * @create 2026-03-24
 * @description: 校验用户拼团列表批量查询逻辑，避免 N+1 回表
 */
@ExtendWith(MockitoExtension.class)
class GroupQueryServiceTest {

    @Mock
    private GroupStorageComponent groupStorageComponent;

    @InjectMocks
    private GroupQueryService groupQueryService;

    /**
     * 用户拼团列表应按用户成员记录先批量查团ID，再一次性回表加载摘要，避免逐团查详情。
     */
    @Test
    void shouldBatchLoadUserGroupsWithoutNPlusOneQueries() {
        when(groupStorageComponent.findMongoUserMembers(1001L)).thenReturn(Arrays.asList(
                buildUserMember("group-2", 102L, "ORDER-2", new Date(2L)),
                buildUserMember("group-1", 101L, "ORDER-1", new Date(1L)),
                buildUserMember("group-1", 101L, "ORDER-1-DUP", new Date(0L))
        ));
        when(groupStorageComponent.findMongoGroupSummaries(Arrays.asList("group-2", "group-1"))).thenReturn(Arrays.asList(
                buildSummaryInstance("group-1", 101L, "ORDER-1"),
                buildSummaryInstance("group-2", 102L, "ORDER-2")
        ));

        List<GroupInstanceVO> result = groupQueryService.getUserGroups(1001L);

        assertEquals(2, result.size());
        assertEquals("group-2", result.get(0).getId());
        assertEquals("group-1", result.get(1).getId());
        assertEquals(1, result.get(0).getMembers().size());
        assertEquals("ORDER-2", result.get(0).getMembers().get(0).getOrderId());
        verify(groupStorageComponent).findMongoUserMembers(1001L);
        verify(groupStorageComponent).findMongoGroupSummaries(Arrays.asList("group-2", "group-1"));
        verify(groupStorageComponent, never()).findMongoGroupInstance("group-1");
    }

    /**
     * 当用户没有任何拼团记录时，应直接返回空列表。
     */
    @Test
    void shouldReturnEmptyListWhenUserHasNoGroups() {
        when(groupStorageComponent.findMongoUserMembers(1001L)).thenReturn(Collections.emptyList());

        List<GroupInstanceVO> result = groupQueryService.getUserGroups(1001L);

        assertEquals(0, result.size());
        verify(groupStorageComponent).findMongoUserMembers(1001L);
        verify(groupStorageComponent, never()).findMongoGroupSummaries(Collections.emptyList());
    }

    /**
     * 构建用户参与记录。
     */
    private GroupMember buildUserMember(String groupId, Long userId, String orderId, Date joinTime) {
        GroupMember member = new GroupMember();
        member.setGroupInstanceId(groupId);
        member.setUserId(userId);
        member.setOrderId(orderId);
        member.setJoinTime(joinTime);
        return member;
    }

    /**
     * 构建团摘要实例。
     */
    private GroupInstance buildSummaryInstance(String groupId, Long userId, String orderId) {
        GroupInstance instance = new GroupInstance();
        instance.setId(groupId);
        instance.setActivityId("activity-1");
        instance.setLeaderUserId(userId);
        instance.setStatus("OPEN");
        instance.setRequiredSize(2);
        instance.setCurrentSize(1);
        instance.setRemainingSlots(1);
        instance.setExpireTime(new Date());
        instance.setSpuId(1001L);
        GroupInstance.GroupMemberInfo memberInfo = new GroupInstance.GroupMemberInfo();
        memberInfo.setUserId(userId);
        memberInfo.setOrderId(orderId);
        memberInfo.setJoinTime(new Date());
        instance.setMembers(Collections.singletonList(memberInfo));
        return instance;
    }
}
