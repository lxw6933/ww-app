package com.ww.mall.promotion.service.group.impl;

import com.ww.app.redis.component.RedissonComponent;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.entity.group.GroupTrade;
import com.ww.mall.promotion.enums.GroupTradeStatus;
import com.ww.mall.promotion.enums.GroupTradeType;
import com.ww.mall.promotion.key.GroupRedisKeyBuilder;
import com.ww.mall.promotion.mq.GroupOrderPaidMessage;
import com.ww.mall.promotion.service.group.GroupInstanceService;
import com.ww.mall.promotion.service.group.command.CreateGroupCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 拼团支付消息编排服务测试。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 校验支付成功消息驱动的开团编排和重复消息幂等处理
 */
@ExtendWith(MockitoExtension.class)
class GroupTradeServiceImplTest {

    @Mock
    private GroupInstanceService groupInstanceService;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private GroupRedisKeyBuilder groupRedisKeyBuilder;

    @Mock
    private RedissonComponent redissonComponent;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @InjectMocks
    private GroupTradeServiceImpl groupTradeService;

    @Test
    @DisplayName("支付成功消息到达时应驱动正式开团并保存交易单")
    void handleOrderPaid_shouldCreateGroupWhenStartMessageArrives() throws InterruptedException {
        GroupOrderPaidMessage message = buildStartMessage();
        GroupInstanceVO groupInstanceVO = buildGroupInstanceVO("GROUP_001", "ACT_001");

        when(groupRedisKeyBuilder.buildTradeLockKey("ORDER_001")).thenReturn("group:trade:lock:ORDER_001");
        when(redissonComponent.getRedissonClient()).thenReturn(redissonClient);
        when(redissonClient.getLock("group:trade:lock:ORDER_001")).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(mongoTemplate.findOne(any(Query.class), eq(GroupTrade.class))).thenReturn(null);
        when(groupInstanceService.createGroup(any(CreateGroupCommand.class))).thenReturn(groupInstanceVO);

        GroupInstanceVO result = groupTradeService.handleOrderPaid(message);

        assertNotNull(result);
        assertEquals("GROUP_001", result.getId());
        verify(groupInstanceService).createGroup(any(CreateGroupCommand.class));
        verify(mongoTemplate, times(2)).save(any(GroupTrade.class));

        ArgumentCaptor<CreateGroupCommand> captor = ArgumentCaptor.forClass(CreateGroupCommand.class);
        verify(groupInstanceService).createGroup(captor.capture());
        assertEquals(10001L, captor.getValue().getUserId());
    }

    @Test
    @DisplayName("重复支付成功消息命中成功交易单时不应重复开团")
    void handleOrderPaid_shouldReturnExistingGroupWhenTradeAlreadySucceeded() throws InterruptedException {
        GroupOrderPaidMessage message = buildStartMessage();
        GroupTrade trade = new GroupTrade();
        trade.setStatus(GroupTradeStatus.SUCCESS);
        trade.setGroupId("GROUP_001");

        when(groupRedisKeyBuilder.buildTradeLockKey("ORDER_001")).thenReturn("group:trade:lock:ORDER_001");
        when(redissonComponent.getRedissonClient()).thenReturn(redissonClient);
        when(redissonClient.getLock("group:trade:lock:ORDER_001")).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(mongoTemplate.findOne(any(Query.class), eq(GroupTrade.class))).thenReturn(trade);
        when(groupInstanceService.getGroupDetail("GROUP_001")).thenReturn(buildGroupInstanceVO("GROUP_001", "ACT_001"));

        GroupInstanceVO result = groupTradeService.handleOrderPaid(message);

        assertNotNull(result);
        assertEquals("GROUP_001", result.getId());
        verify(groupInstanceService, never()).createGroup(any(CreateGroupCommand.class));
        verify(groupInstanceService).getGroupDetail("GROUP_001");
    }

    private GroupOrderPaidMessage buildStartMessage() {
        GroupOrderPaidMessage message = new GroupOrderPaidMessage();
        message.setTraceId("TRACE_001");
        message.setTradeType(GroupTradeType.START);
        message.setActivityId("ACT_001");
        message.setUserId(10001L);
        message.setOrderId("ORDER_001");
        message.setSpuId(1001L);
        message.setSkuId(2001L);
        message.setOrderInfo("{\"spuId\":1001,\"skuId\":2001,\"payAmount\":99.00}");
        return message;
    }

    private GroupInstanceVO buildGroupInstanceVO(String groupId, String activityId) {
        GroupInstanceVO groupInstanceVO = new GroupInstanceVO();
        groupInstanceVO.setId(groupId);
        groupInstanceVO.setActivityId(activityId);
        return groupInstanceVO;
    }
}
