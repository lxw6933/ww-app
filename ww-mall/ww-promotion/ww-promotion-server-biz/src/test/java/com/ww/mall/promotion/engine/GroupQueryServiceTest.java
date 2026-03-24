package com.ww.mall.promotion.engine;

import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.entity.group.GroupInstance;
import com.ww.mall.promotion.entity.group.GroupMember;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private GroupRedisStateReader groupRedisStateReader;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private GroupQueryService groupQueryService;

    /**
     * 用户拼团列表应按用户成员记录先批量查团ID，再一次性回表加载摘要，避免逐团查详情。
     */
    @Test
    void shouldBatchLoadUserGroupsWithoutNPlusOneQueries() {
        when(mongoTemplate.find(any(Query.class), eq(GroupMember.class))).thenReturn(Arrays.asList(
                buildUserMember("group-2", 102L, "ORDER-2", new Date(2L)),
                buildUserMember("group-1", 101L, "ORDER-1", new Date(1L)),
                buildUserMember("group-1", 101L, "ORDER-1-DUP", new Date(0L))
        ));
        when(mongoTemplate.find(any(Query.class), eq(GroupInstance.class))).thenReturn(Arrays.asList(
                buildSummaryInstance("group-1", 101L, "ORDER-1"),
                buildSummaryInstance("group-2", 102L, "ORDER-2")
        ));

        List<GroupInstanceVO> result = groupQueryService.getUserGroups(1001L);

        assertEquals(2, result.size());
        assertEquals("group-2", result.get(0).getId());
        assertEquals("group-1", result.get(1).getId());
        assertEquals(1, result.get(0).getMembers().size());
        assertEquals("ORDER-2", result.get(0).getMembers().get(0).getOrderId());
        verify(mongoTemplate, times(1)).find(any(Query.class), eq(GroupMember.class));
        verify(mongoTemplate, times(1)).find(any(Query.class), eq(GroupInstance.class));
        verify(mongoTemplate, never()).findOne(any(Query.class), eq(GroupInstance.class));
    }

    /**
     * 当用户没有任何拼团记录时，应直接返回空列表。
     */
    @Test
    void shouldReturnEmptyListWhenUserHasNoGroups() {
        when(mongoTemplate.find(any(Query.class), eq(GroupMember.class))).thenReturn(Collections.emptyList());

        List<GroupInstanceVO> result = groupQueryService.getUserGroups(1001L);

        assertEquals(0, result.size());
        verify(mongoTemplate, times(1)).find(any(Query.class), eq(GroupMember.class));
        verify(mongoTemplate, never()).find(any(Query.class), eq(GroupInstance.class));
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
        memberInfo.setIsLeader(Boolean.TRUE);
        instance.setMembers(Collections.singletonList(memberInfo));
        return instance;
    }
}
