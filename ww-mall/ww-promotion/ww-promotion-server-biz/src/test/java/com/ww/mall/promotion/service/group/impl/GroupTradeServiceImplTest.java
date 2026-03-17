package com.ww.mall.promotion.service.group.impl;

import com.ww.mall.promotion.controller.app.group.req.CreateGroupRequest;
import com.ww.mall.promotion.controller.app.group.req.GroupPaymentCallbackRequest;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.entity.group.GroupTrade;
import com.ww.mall.promotion.enums.GroupTradeStatus;
import com.ww.mall.promotion.enums.GroupTradeType;
import com.ww.mall.promotion.key.GroupRedisKeyBuilder;
import com.ww.mall.promotion.service.group.GroupInstanceService;
import com.ww.mall.promotion.service.group.support.GroupFlowLogSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 拼团支付回调编排服务测试。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 校验支付回调驱动的开团编排和重复回调幂等处理
 */
@ExtendWith(MockitoExtension.class)
class GroupTradeServiceImplTest {

    @Mock
    private GroupInstanceService groupInstanceService;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private GroupRedisKeyBuilder groupRedisKeyBuilder;

    @Mock
    private GroupFlowLogSupport groupFlowLogSupport;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private GroupTradeServiceImpl groupTradeService;

    @Test
    @DisplayName("支付回调成功时应驱动正式开团并保存交易单")
    void handlePaymentCallback_shouldCreateGroupWhenStartCallbackArrives() {
        GroupPaymentCallbackRequest request = buildStartRequest();
        GroupInstanceVO groupInstanceVO = buildGroupInstanceVO("GROUP_001", "ACT_001");

        when(groupRedisKeyBuilder.buildPaymentCallbackLockKey("PAY_001")).thenReturn("group:callback:lock:PAY_001");
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("group:callback:lock:PAY_001"), eq("TRACE_001"), any(Duration.class)))
                .thenReturn(Boolean.TRUE);
        when(mongoTemplate.findOne(any(Query.class), eq(GroupTrade.class))).thenReturn(null);
        when(groupInstanceService.createGroup(any(CreateGroupRequest.class))).thenReturn(groupInstanceVO);

        GroupInstanceVO result = groupTradeService.handlePaymentCallback(request);

        assertNotNull(result);
        assertEquals("GROUP_001", result.getId());
        verify(groupInstanceService).createGroup(any(CreateGroupRequest.class));
        verify(mongoTemplate, times(2)).save(any(GroupTrade.class));

        ArgumentCaptor<CreateGroupRequest> captor = ArgumentCaptor.forClass(CreateGroupRequest.class);
        verify(groupInstanceService).createGroup(captor.capture());
        assertEquals("PAY_001", captor.getValue().getPayTransId());
        assertEquals(10001L, captor.getValue().getUserId());
    }

    @Test
    @DisplayName("重复支付回调命中成功交易单时不应重复开团")
    void handlePaymentCallback_shouldReturnExistingGroupWhenTradeAlreadySucceeded() {
        GroupPaymentCallbackRequest request = buildStartRequest();
        GroupTrade trade = new GroupTrade();
        trade.setStatus(GroupTradeStatus.SUCCESS);
        trade.setGroupId("GROUP_001");

        when(groupRedisKeyBuilder.buildPaymentCallbackLockKey("PAY_001")).thenReturn("group:callback:lock:PAY_001");
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("group:callback:lock:PAY_001"), eq("TRACE_001"), any(Duration.class)))
                .thenReturn(Boolean.TRUE);
        when(mongoTemplate.findOne(any(Query.class), eq(GroupTrade.class))).thenReturn(trade);
        when(groupInstanceService.getGroupDetail("GROUP_001")).thenReturn(buildGroupInstanceVO("GROUP_001", "ACT_001"));

        GroupInstanceVO result = groupTradeService.handlePaymentCallback(request);

        assertNotNull(result);
        assertEquals("GROUP_001", result.getId());
        verify(groupInstanceService, never()).createGroup(any(CreateGroupRequest.class));
        verify(groupInstanceService).getGroupDetail("GROUP_001");
    }

    private GroupPaymentCallbackRequest buildStartRequest() {
        GroupPaymentCallbackRequest request = new GroupPaymentCallbackRequest();
        request.setTraceId("TRACE_001");
        request.setTradeType(GroupTradeType.START);
        request.setActivityId("ACT_001");
        request.setUserId(10001L);
        request.setOrderId("ORDER_001");
        request.setPayTransId("PAY_001");
        request.setOrderInfo("{\"spuId\":1001,\"skuId\":2001}");
        return request;
    }

    private GroupInstanceVO buildGroupInstanceVO(String groupId, String activityId) {
        GroupInstanceVO groupInstanceVO = new GroupInstanceVO();
        groupInstanceVO.setId(groupId);
        groupInstanceVO.setActivityId(activityId);
        return groupInstanceVO;
    }
}
