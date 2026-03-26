package com.ww.mall.promotion.service.group.impl;

import com.mongodb.client.result.UpdateResult;
import com.ww.mall.promotion.component.GroupStorageComponent;
import com.ww.mall.promotion.constants.RedisChannelConstant;
import com.ww.mall.promotion.controller.admin.group.req.GroupActivityBO;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.enums.GroupEnabledStatus;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 拼团活动服务实现测试。
 *
 * @author ww
 * @create 2026-03-26
 * @description: 校验活动动态查询、展示规则选择以及活动统计归档逻辑
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
        assertEquals(1, queryObject.get("enabled"));
        assertTrue(queryObject.containsKey("startTime"));
        assertTrue(queryObject.containsKey("endTime"));
    }

    /**
     * 创建活动时，应仅从启用中的 SKU 规则里挑选默认展示规则。
     */
    @Test
    void shouldUseEnabledSkuRuleAsDisplayRule() {
        when(mongoTemplate.save(any(GroupActivity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GroupActivityBO bo = buildBaseActivityBO();
        bo.setSkuRules(Arrays.asList(
                buildSkuRule(101L, "9.90", "19.90", GroupEnabledStatus.DISABLED.getCode()),
                buildSkuRule(102L, "29.90", "39.90", GroupEnabledStatus.ENABLED.getCode()),
                buildSkuRule(103L, "15.90", "25.90", GroupEnabledStatus.ENABLED.getCode())
        ));

        GroupActivity activity = groupActivityService.createActivity(bo);

        assertEquals(103L, activity.getSkuId());
        assertEquals(new BigDecimal("15.90"), activity.getGroupPrice());
        assertEquals(new BigDecimal("25.90"), activity.getOriginalPrice());
        assertEquals(0L, activity.getOpenGroupCount());
        assertEquals(0L, activity.getJoinMemberCount());
        assertFalse(activity.getStatsSettled());
        assertNull(activity.getStatsSettledTime());
        verify(groupStorageComponent).fillActivityStatistics(activity);
    }

    /**
     * 当所有 SKU 规则均被禁用时，应清空兼容展示字段，避免保留旧值误导调用方。
     */
    @Test
    void shouldClearDisplayFieldsWhenAllSkuRulesDisabled() {
        when(mongoTemplate.save(any(GroupActivity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GroupActivityBO bo = buildBaseActivityBO();
        bo.setSkuRules(Arrays.asList(
                buildSkuRule(101L, "19.90", "29.90", GroupEnabledStatus.DISABLED.getCode()),
                buildSkuRule(102L, "15.90", "25.90", GroupEnabledStatus.DISABLED.getCode())
        ));

        GroupActivity activity = groupActivityService.createActivity(bo);

        assertNull(activity.getSkuId());
        assertNull(activity.getGroupPrice());
        assertNull(activity.getOriginalPrice());
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
        bo.setSpuId(1001L);
        bo.setRequiredSize(2);
        bo.setExpireHours(24);
        bo.setStartTime(new Date(nowMillis + 60_000L));
        bo.setEndTime(new Date(nowMillis + 3_600_000L));
        bo.setLimitPerUser(1);
        bo.setImageUrl("https://test.example.com/group.png");
        bo.setSortWeight(100);
        return bo;
    }

    /**
     * 构建 SKU 规则请求。
     *
     * @param skuId SKU ID
     * @param groupPrice 拼团价
     * @param originalPrice 原价
     * @param enabled 启用状态
     * @return SKU 规则请求
     */
    private GroupActivityBO.GroupSkuRuleBO buildSkuRule(Long skuId, String groupPrice, String originalPrice, Integer enabled) {
        GroupActivityBO.GroupSkuRuleBO skuRuleBO = new GroupActivityBO.GroupSkuRuleBO();
        skuRuleBO.setSkuId(skuId);
        skuRuleBO.setGroupPrice(new BigDecimal(groupPrice));
        skuRuleBO.setOriginalPrice(new BigDecimal(originalPrice));
        skuRuleBO.setEnabled(enabled);
        return skuRuleBO;
    }
}
