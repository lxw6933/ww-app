package com.ww.mall.promotion.service.group.impl;

import com.mongodb.client.result.UpdateResult;
import com.ww.mall.promotion.component.GroupStorageComponent;
import com.ww.mall.promotion.constants.RedisChannelConstant;
import com.ww.mall.promotion.controller.admin.group.req.GroupActivityBO;
import com.ww.mall.promotion.entity.group.GroupActivity;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 拼团活动服务实现测试。
 *
 * @author ww
 * @create 2026-03-26
 * @description: 校验活动动态查询、多SPU配置组装以及活动统计归档逻辑
 */
@ExtendWith(MockitoExtension.class)
class GroupActivityServiceImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private GroupStorageComponent groupStorageComponent;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private GroupActivityServiceImpl groupActivityService;

    /**
     * 查询进行中活动时，不应再依赖持久化 status 字段。
     */
    @Test
    void shouldBuildActiveQueryWithoutPersistentStatusField() {
        when(mongoTemplate.find(any(Query.class), eq(GroupActivity.class))).thenReturn(Collections.emptyList());

        groupActivityService.listActiveActivities();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(GroupActivity.class));
        Document queryObject = queryCaptor.getValue().getQueryObject();
        assertFalse(queryObject.containsKey("status"));
        assertEquals(true, queryObject.get("enabled"));
        assertTrue(queryObject.containsKey("startTime"));
        assertTrue(queryObject.containsKey("endTime"));
    }

    /**
     * 创建活动时，应保留多个 SPU 配置及其 SKU 规则。
     */
    @Test
    void shouldCreateActivityWithMultipleSpuConfigs() {
        when(mongoTemplate.save(any(GroupActivity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GroupActivityBO bo = buildBaseActivityBO();
        bo.setSpuConfigs(Arrays.asList(
                buildSpuConfig(1001L,
                        buildSkuRule(101L, "29.90", true),
                        buildSkuRule(102L, "19.90", false)),
                buildSpuConfig(1002L,
                        buildSkuRule(201L, "39.90", true))
        ));

        GroupActivity activity = groupActivityService.createActivity(bo);

        assertEquals(2, activity.getSpuConfigs().size());
        assertEquals(1001L, activity.getSpuConfigs().get(0).getSpuId());
        assertEquals(2, activity.getSpuConfigs().get(0).getSkuRules().size());
        assertEquals(201L, activity.getSpuConfigs().get(1).getSkuRules().get(0).getSkuId());
        assertEquals(0L, activity.getOpenGroupCount());
        assertEquals(0L, activity.getJoinMemberCount());
        assertFalse(activity.getStatsSettled());
        verify(groupStorageComponent).fillActivityStatistics(activity);
    }

    /**
     * 按 SPU 查询活动时，应命中嵌套 SPU 字段并仅返回匹配 SPU 的配置切片。
     */
    @Test
    void shouldQueryActivitiesByNestedSpuIdAndTrimUnmatchedSpuConfigs() {
        GroupActivity activity = new GroupActivity();
        activity.setId("activity-1");
        activity.setSpuConfigs(Arrays.asList(
                buildActivitySpuConfig(1001L, buildActivitySkuRule(101L, "29.90", true)),
                buildActivitySpuConfig(1002L, buildActivitySkuRule(201L, "39.90", true))
        ));
        when(mongoTemplate.find(any(Query.class), eq(GroupActivity.class))).thenReturn(Collections.singletonList(activity));

        java.util.List<GroupActivity> activities = groupActivityService.getActivitiesBySpuId(1002L);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(GroupActivity.class));
        Document queryObject = queryCaptor.getValue().getQueryObject();
        assertEquals(1002L, queryObject.get("spuConfigs.spuId"));
        assertEquals(1, activities.size());
        assertEquals(1, activities.get(0).getSpuConfigs().size());
        assertEquals(1002L, activities.get(0).getSpuConfigs().get(0).getSpuId());
    }

    /**
     * 活动结束后，应把 Redis 中的最终统计值落库，并清理运行态缓存。
     */
    @Test
    void shouldSettleExpiredActivityStatisticsAndClearRedisKey() {
        GroupActivity activity = new GroupActivity();
        activity.setId("activity-1");
        activity.setEndTime(new Date(System.currentTimeMillis() - 60_000L));
        activity.setOpenGroupCount(0L);
        activity.setJoinMemberCount(0L);
        when(mongoTemplate.find(any(Query.class), eq(GroupActivity.class))).thenReturn(Collections.singletonList(activity));
        doAnswer(invocation -> {
            GroupActivity target = invocation.getArgument(0);
            target.setOpenGroupCount(8L);
            target.setJoinMemberCount(15L);
            return null;
        }).when(groupStorageComponent).fillActivityStatistics(any(GroupActivity.class));
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(GroupActivity.class)))
                .thenReturn(UpdateResult.acknowledged(1L, 1L, null));

        int settledCount = groupActivityService.settleExpiredActivityStatistics();

        assertEquals(1, settledCount);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(any(Query.class), updateCaptor.capture(), eq(GroupActivity.class));
        Document updateObject = updateCaptor.getValue().getUpdateObject();
        Document setObject = (Document) updateObject.get("$set");
        assertEquals(8L, setObject.get("openGroupCount"));
        assertEquals(15L, setObject.get("joinMemberCount"));
        assertEquals(true, setObject.get("statsSettled"));
        verify(groupStorageComponent).clearActivityStatistics("activity-1");
        verify(stringRedisTemplate).convertAndSend(RedisChannelConstant.GROUP_ACTIVITY_CACHE_CHANNEL, "activity-1");
    }

    /**
     * 没有待归档活动时，应直接跳过，不触发 Redis 清理和缓存失效。
     */
    @Test
    void shouldSkipWhenNoExpiredActivitiesToSettle() {
        when(mongoTemplate.find(any(Query.class), eq(GroupActivity.class))).thenReturn(Collections.emptyList());

        int settledCount = groupActivityService.settleExpiredActivityStatistics();

        assertEquals(0, settledCount);
        verify(groupStorageComponent, never()).clearActivityStatistics(any(String.class));
        verify(stringRedisTemplate, never()).convertAndSend(any(String.class), any(String.class));
    }

    /**
     * 构建基础活动请求。
     *
     * @return 活动请求
     */
    private GroupActivityBO buildBaseActivityBO() {
        long nowMillis = System.currentTimeMillis();
        GroupActivityBO bo = new GroupActivityBO();
        bo.setName("测试拼团活动");
        bo.setDescription("用于单元测试");
        bo.setRequiredSize(2);
        bo.setExpireHours(24);
        bo.setStartTime(new Date(nowMillis + 60_000L));
        bo.setEndTime(new Date(nowMillis + 3_600_000L));
        bo.setLimitPerUser(1);
        return bo;
    }

    /**
     * 构建 SKU 规则请求。
     *
     * @param skuId SKU ID
     * @param groupPrice 拼团价
     * @param enabled 启用状态
     * @return SKU 规则请求
     */
    private GroupActivityBO.GroupSkuRuleBO buildSkuRule(Long skuId, String groupPrice, Boolean enabled) {
        GroupActivityBO.GroupSkuRuleBO skuRuleBO = new GroupActivityBO.GroupSkuRuleBO();
        skuRuleBO.setSkuId(skuId);
        skuRuleBO.setGroupPrice(new BigDecimal(groupPrice));
        skuRuleBO.setEnabled(enabled);
        return skuRuleBO;
    }

    /**
     * 构建 SPU 配置请求。
     *
     * @param spuId SPU ID
     * @param skuRules SKU 规则
     * @return SPU 配置请求
     */
    private GroupActivityBO.GroupSpuConfigBO buildSpuConfig(Long spuId, GroupActivityBO.GroupSkuRuleBO... skuRules) {
        GroupActivityBO.GroupSpuConfigBO spuConfigBO = new GroupActivityBO.GroupSpuConfigBO();
        spuConfigBO.setSpuId(spuId);
        spuConfigBO.setSkuRules(Arrays.asList(skuRules));
        return spuConfigBO;
    }

    /**
     * 构建活动 SPU 配置。
     *
     * @param spuId SPU ID
     * @param skuRules SKU 规则
     * @return SPU 配置
     */
    private GroupActivity.GroupSpuConfig buildActivitySpuConfig(Long spuId, GroupActivity.GroupSkuRule... skuRules) {
        GroupActivity.GroupSpuConfig spuConfig = new GroupActivity.GroupSpuConfig();
        spuConfig.setSpuId(spuId);
        spuConfig.setSkuRules(Arrays.asList(skuRules));
        return spuConfig;
    }

    /**
     * 构建活动 SKU 规则。
     *
     * @param skuId SKU ID
     * @param groupPrice 拼团价
     * @param enabled 是否启用
     * @return SKU 规则
     */
    private GroupActivity.GroupSkuRule buildActivitySkuRule(Long skuId, String groupPrice, Boolean enabled) {
        GroupActivity.GroupSkuRule skuRule = new GroupActivity.GroupSkuRule();
        skuRule.setSkuId(skuId);
        skuRule.setGroupPrice(new BigDecimal(groupPrice));
        skuRule.setEnabled(enabled);
        return skuRule;
    }
}
