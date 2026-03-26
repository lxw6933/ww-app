package com.ww.mall.promotion.engine;

import com.ww.app.common.common.AppPage;
import com.ww.app.common.common.AppPageResult;
import com.ww.mall.promotion.component.GroupStorageComponent;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.entity.group.GroupInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 拼团查询服务测试。
 *
 * @author ww
 * @create 2026-03-24
 * @description: 校验用户拼团列表批量查询与分页查询逻辑
 */
@ExtendWith(MockitoExtension.class)
class GroupQueryServiceTest {

    @Mock
    private GroupStorageComponent groupStorageComponent;

    @InjectMocks
    private GroupQueryService groupQueryService;

    /**
     * 分页查询用户拼团列表时，应直接基于去重后的团ID分页，再批量回表读取摘要。
     */
    @Test
    void shouldPageUserGroupsByDistinctGroupIds() {
        AppPage page = new AppPage(2, 1);
        when(groupStorageComponent.countMongoUserGroups(1001L)).thenReturn(3L);
        when(groupStorageComponent.findMongoUserGroupIds(1001L, 2, 1)).thenReturn(Collections.singletonList("group-2"));
        when(groupStorageComponent.findMongoGroupSummaries(Collections.singletonList("group-2"))).thenReturn(
                Collections.singletonList(buildSummaryInstance("group-2", 102L, "ORDER-2"))
        );

        AppPageResult<GroupInstanceVO> result = groupQueryService.getUserGroups(1001L, page);

        assertEquals(3, result.getTotalCount());
        assertEquals(3, result.getTotalPage());
        assertEquals(1, result.getResult().size());
        assertEquals("group-2", result.getResult().get(0).getId());
        verify(groupStorageComponent).countMongoUserGroups(1001L);
        verify(groupStorageComponent).findMongoUserGroupIds(1001L, 2, 1);
        verify(groupStorageComponent).findMongoGroupSummaries(Collections.singletonList("group-2"));
    }

    /**
     * 分页查询活动拼团列表时，应命中分页摘要查询而非全量列表查询。
     */
    @Test
    void shouldPageActivityGroups() {
        AppPage page = new AppPage(1, 2);
        when(groupStorageComponent.countMongoActivityGroups("activity-1", "OPEN")).thenReturn(2L);
        when(groupStorageComponent.findMongoActivityGroupSummaries("activity-1", "OPEN", 1, 2)).thenReturn(Arrays.asList(
                buildSummaryInstance("group-2", 102L, "ORDER-2"),
                buildSummaryInstance("group-1", 101L, "ORDER-1")
        ));

        AppPageResult<GroupInstanceVO> result = groupQueryService.getActivityGroups("activity-1", "OPEN", page);

        assertEquals(2, result.getTotalCount());
        assertEquals(1, result.getTotalPage());
        assertEquals(2, result.getResult().size());
        assertEquals("group-2", result.getResult().get(0).getId());
        verify(groupStorageComponent).countMongoActivityGroups("activity-1", "OPEN");
        verify(groupStorageComponent).findMongoActivityGroupSummaries("activity-1", "OPEN", 1, 2);
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
