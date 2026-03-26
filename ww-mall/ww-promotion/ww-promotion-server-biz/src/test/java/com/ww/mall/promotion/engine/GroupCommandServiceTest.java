package com.ww.mall.promotion.engine;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.app.common.exception.ApiException;
import com.ww.app.rabbitmq.RabbitMqPublisher;
import com.ww.mall.promotion.component.GroupStorageComponent;
import com.ww.mall.promotion.engine.model.GroupCacheSnapshot;
import com.ww.mall.promotion.engine.model.GroupCommandResult;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.entity.group.GroupInstance;
import com.ww.mall.promotion.entity.group.GroupMember;
import com.ww.mall.promotion.enums.GroupEnabledStatus;
import com.ww.mall.promotion.enums.GroupTradeType;
import com.ww.mall.promotion.mq.GroupMqConstant;
import com.ww.mall.promotion.mq.GroupOrderPaidMessage;
import com.ww.mall.promotion.mq.GroupRefundRequestMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 拼团命令服务测试。
 *
 * @author ww
 * @create 2026-03-25
 * @description: 校验内部状态变更消息发送失败时会退化执行本地 Mongo 投影兜底
 */
@ExtendWith(MockitoExtension.class)
class GroupCommandServiceTest {

    @Mock
    private LoadingCache<String, GroupActivity> groupActivityCache;

    @Mock
    private GroupStorageComponent groupStorageComponent;

    @Mock
    private RabbitMqPublisher rabbitMqPublisher;

    @InjectMocks
    private GroupCommandService groupCommandService;

    /**
     * 当内部状态变更消息发送成功时，不应触发本地投影兜底。
     */
    @Test
    void shouldNotSyncProjectionWhenStateChangedMessageSentSuccessfully() {
        ReflectionTestUtils.invokeMethod(groupCommandService, "afterStateChanged", "group-1", 1L);

        verify(rabbitMqPublisher).sendMsg(
                eq(GroupMqConstant.GROUP_EXCHANGE),
                eq(GroupMqConstant.GROUP_STATE_CHANGED_KEY),
                any()
        );
        verify(groupStorageComponent, never()).syncProjection(any(String.class));
    }

    /**
     * 当内部状态变更消息发送失败时，应立即同步一次 Mongo 投影作为兜底。
     */
    @Test
    void shouldSyncProjectionWhenStateChangedMessageSendFails() {
        doThrow(new RuntimeException("mq down")).when(rabbitMqPublisher).sendMsg(
                eq(GroupMqConstant.GROUP_EXCHANGE),
                eq(GroupMqConstant.GROUP_STATE_CHANGED_KEY),
                any()
        );

        ReflectionTestUtils.invokeMethod(groupCommandService, "afterStateChanged", "group-1", 1L);

        verify(groupStorageComponent).syncProjection("group-1");
    }

    /**
     * 当支付成功后被拼团业务规则拒绝时，应发送退款补偿消息而不是继续抛错重试。
     */
    @Test
    void shouldRequestRefundWhenPaidJoinRejectedByBusinessRule() {
        GroupOrderPaidMessage message = buildJoinPaidMessage();
        GroupActivity activity = buildJoinableActivity();
        activity.setEnabled(GroupEnabledStatus.DISABLED.getCode());

        when(groupStorageComponent.requireGroupSnapshot("group-1")).thenReturn(buildBeforeJoinSnapshot());
        when(groupActivityCache.get("activity-1")).thenReturn(activity);

        assertNull(groupCommandService.handleOrderPaid(message));

        ArgumentCaptor<GroupRefundRequestMessage> captor = ArgumentCaptor.forClass(GroupRefundRequestMessage.class);
        verify(rabbitMqPublisher).sendMsg(
                eq(GroupMqConstant.GROUP_EXCHANGE),
                eq(GroupMqConstant.GROUP_REFUND_REQUEST_KEY),
                captor.capture()
        );
        GroupRefundRequestMessage refundMessage = captor.getValue();
        assertEquals("group-1", refundMessage.getGroupId());
        assertEquals("order-1", refundMessage.getOrderId());
        assertEquals("GROUP_JOIN_REJECTED", refundMessage.getRefundScene());
        assertEquals(new BigDecimal("99.00"), refundMessage.getRefundAmount());
    }

    /**
     * 当支付成功后参团命中“团已关闭”这类确定性终态时，也应发送退款补偿消息。
     */
    @Test
    void shouldRequestRefundWhenPaidJoinRejectedByClosedGroup() {
        GroupOrderPaidMessage message = buildJoinPaidMessage();
        GroupCommandResult result = new GroupCommandResult();
        result.setSuccess(false);
        result.setGroupId("group-1");
        result.setGroupStatus("FAILED");
        result.setFailReason("-4");

        when(groupStorageComponent.requireGroupSnapshot("group-1")).thenReturn(buildBeforeJoinSnapshot());
        when(groupActivityCache.get("activity-1")).thenReturn(buildJoinableActivity());
        when(groupStorageComponent.joinGroup(any(GroupActivity.class), any(), anyLong())).thenReturn(result);

        assertNull(groupCommandService.handleOrderPaid(message));

        verify(rabbitMqPublisher).sendMsg(
                eq(GroupMqConstant.GROUP_EXCHANGE),
                eq(GroupMqConstant.GROUP_REFUND_REQUEST_KEY),
                any()
        );
    }

    /**
     * 当支付成功后的失败属于系统异常时，不应贸然发送退款补偿消息。
     */
    @Test
    void shouldNotRequestRefundWhenPaidFailureIsUnknown() {
        GroupOrderPaidMessage message = buildJoinPaidMessage();
        when(groupStorageComponent.requireGroupSnapshot("group-1")).thenThrow(new ApiException(500, "系统异常"));

        assertThrows(ApiException.class, () -> groupCommandService.handleOrderPaid(message));

        verify(rabbitMqPublisher, never()).sendMsg(
                eq(GroupMqConstant.GROUP_EXCHANGE),
                eq(GroupMqConstant.GROUP_REFUND_REQUEST_KEY),
                any()
        );
    }

    /**
     * 当拼团过期失败后，应对所有待退款成员发送退款补偿申请。
     */
    @Test
    void shouldRequestRefundForPendingMembersWhenGroupExpired() {
        when(groupStorageComponent.requireGroupSnapshot("group-1"))
                .thenReturn(buildBeforeJoinSnapshot(), buildFailedSnapshot());
        when(groupStorageComponent.expireGroup(eq("group-1"), eq("超时未成团"), anyLong())).thenReturn(1);

        groupCommandService.expireGroup("group-1", "超时未成团");

        verify(rabbitMqPublisher).sendMsg(
                eq(GroupMqConstant.GROUP_EXCHANGE),
                eq(GroupMqConstant.GROUP_STATE_CHANGED_KEY),
                any()
        );
        ArgumentCaptor<GroupRefundRequestMessage> captor = ArgumentCaptor.forClass(GroupRefundRequestMessage.class);
        verify(rabbitMqPublisher, times(1)).sendMsg(
                eq(GroupMqConstant.GROUP_EXCHANGE),
                eq(GroupMqConstant.GROUP_REFUND_REQUEST_KEY),
                captor.capture()
        );
        GroupRefundRequestMessage refundMessage = captor.getValue();
        assertEquals("group-1", refundMessage.getGroupId());
        assertEquals("order-1", refundMessage.getOrderId());
        assertEquals("GROUP_FAILED_REFUND", refundMessage.getRefundScene());
        assertEquals("超时未成团", refundMessage.getReason());
    }

    /**
     * 构造参团支付成功消息。
     *
     * @return 支付消息
     */
    private GroupOrderPaidMessage buildJoinPaidMessage() {
        GroupOrderPaidMessage message = new GroupOrderPaidMessage();
        message.setTradeType(GroupTradeType.JOIN);
        message.setGroupId("group-1");
        message.setOrderId("order-1");
        message.setUserId(1L);
        message.setSkuId(1001L);
        message.setPayAmount(new BigDecimal("99.00"));
        return message;
    }

    /**
     * 构造可正常参与的活动。
     *
     * @return 活动
     */
    private GroupActivity buildJoinableActivity() {
        GroupActivity activity = new GroupActivity();
        activity.setId("activity-1");
        activity.setEnabled(GroupEnabledStatus.ENABLED.getCode());
        activity.setRequiredSize(2);
        activity.setExpireHours(24);
        return activity;
    }

    /**
     * 构造参团前快照。
     *
     * @return 团快照
     */
    private GroupCacheSnapshot buildBeforeJoinSnapshot() {
        GroupCacheSnapshot snapshot = new GroupCacheSnapshot();
        GroupInstance instance = new GroupInstance();
        instance.setId("group-1");
        instance.setActivityId("activity-1");
        snapshot.setInstance(instance);
        return snapshot;
    }

    /**
     * 构造失败后的团快照。
     *
     * @return 团快照
     */
    private GroupCacheSnapshot buildFailedSnapshot() {
        GroupCacheSnapshot snapshot = new GroupCacheSnapshot();
        GroupInstance instance = new GroupInstance();
        instance.setId("group-1");
        instance.setActivityId("activity-1");
        instance.setFailReason("超时未成团");
        snapshot.setInstance(instance);

        GroupMember refundMember = new GroupMember();
        refundMember.setUserId(1L);
        refundMember.setOrderId("order-1");
        refundMember.setPayAmount(new BigDecimal("99.00"));
        refundMember.setMemberStatus("FAILED_REFUND_PENDING");

        GroupMember closedMember = new GroupMember();
        closedMember.setUserId(2L);
        closedMember.setOrderId("order-2");
        closedMember.setPayAmount(new BigDecimal("88.00"));
        closedMember.setMemberStatus("LEADER_AFTER_SALE_CLOSED");

        snapshot.setMembers(Arrays.asList(refundMember, closedMember));
        return snapshot;
    }
}
