package com.ww.mall.promotion.service.group.impl;

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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 拼团活动服务测试。
 *
 * @author ww
 * @create 2026-03-24
 * @description: 校验活动状态去字段化后的查询与展示规则
 */
@ExtendWith(MockitoExtension.class)
class GroupActivityServiceImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private GroupActivityServiceImpl groupActivityService;

    /**
     * 查询进行中的活动时，应仅按启用状态和时间窗组装条件，而不再依赖持久化 status 字段。
     */
    @Test
    void shouldBuildActiveQueryWithoutPersistentStatusField() {
        when(mongoTemplate.find(any(Query.class), eq(GroupActivity.class))).thenReturn(Collections.emptyList());
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);

        groupActivityService.listActiveActivities();

        verify(mongoTemplate).find(queryCaptor.capture(), eq(GroupActivity.class));
        Document queryObject = queryCaptor.getValue().getQueryObject();
        assertEquals(1, queryObject.get("enabled"));
        assertFalse(queryObject.containsKey("status"));
        assertTrue(((Document) queryObject.get("startTime")).containsKey("$lte"));
        assertTrue(((Document) queryObject.get("endTime")).containsKey("$gte"));
    }

    /**
     * 创建活动时，应从启用中的 SKU 规则里选择展示价最低的一条，避免展示禁用 SKU 的价格。
     */
    @Test
    void shouldUseEnabledSkuRuleAsDisplayRule() {
        when(mongoTemplate.save(any(GroupActivity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GroupActivityBO bo = buildBaseBo();
        List<GroupActivityBO.GroupSkuRuleBO> skuRules = new ArrayList<>();
        skuRules.add(buildSkuRule(1001L, "59.00", GroupEnabledStatus.DISABLED.getCode()));
        skuRules.add(buildSkuRule(1002L, "79.00", GroupEnabledStatus.ENABLED.getCode()));
        bo.setSkuRules(skuRules);

        GroupActivity activity = groupActivityService.createActivity(bo);

        assertEquals(1002L, activity.getSkuId());
        assertEquals(new BigDecimal("79.00"), activity.getGroupPrice());
        assertEquals(GroupEnabledStatus.ENABLED.getCode(), activity.getEnabled());
    }

    /**
     * 当所有 SKU 规则都被禁用时，应清空兼容展示字段，避免保留旧值误导调用方。
     */
    @Test
    void shouldClearDisplayFieldsWhenAllSkuRulesDisabled() {
        when(mongoTemplate.save(any(GroupActivity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GroupActivityBO bo = buildBaseBo();
        List<GroupActivityBO.GroupSkuRuleBO> skuRules = new ArrayList<>();
        skuRules.add(buildSkuRule(1001L, "59.00", GroupEnabledStatus.DISABLED.getCode()));
        bo.setSkuRules(skuRules);

        GroupActivity activity = groupActivityService.createActivity(bo);

        assertNull(activity.getSkuId());
        assertNull(activity.getGroupPrice());
    }

    /**
     * 构建基础活动请求。
     */
    private GroupActivityBO buildBaseBo() {
        GroupActivityBO bo = new GroupActivityBO();
        bo.setName("测试拼团");
        bo.setDescription("测试描述");
        bo.setSpuId(10L);
        bo.setRequiredSize(2);
        bo.setExpireHours(24);
        bo.setStartTime(new Date(System.currentTimeMillis() + 60_000L));
        bo.setEndTime(new Date(System.currentTimeMillis() + 120_000L));
        return bo;
    }

    /**
     * 构建 SKU 规则。
     */
    private GroupActivityBO.GroupSkuRuleBO buildSkuRule(Long skuId, String groupPrice, Integer enabled) {
        GroupActivityBO.GroupSkuRuleBO ruleBO = new GroupActivityBO.GroupSkuRuleBO();
        ruleBO.setSkuId(skuId);
        ruleBO.setGroupPrice(new BigDecimal(groupPrice));
        ruleBO.setEnabled(enabled);
        return ruleBO;
    }
}
