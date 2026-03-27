package com.ww.mall.promotion.engine;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.app.common.exception.ApiException;
import com.ww.app.rabbitmq.RabbitMqPublisher;
import com.ww.mall.promotion.component.GroupStorageComponent;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.engine.model.GroupCacheSnapshot;
import com.ww.mall.promotion.engine.model.GroupCommandResult;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.entity.group.GroupInstance;
import com.ww.mall.promotion.entity.group.GroupMember;
import com.ww.mall.promotion.enums.GroupCompensationTaskType;
import com.ww.mall.promotion.enums.GroupEnabledStatus;
import com.ww.mall.promotion.enums.GroupStatus;
import com.ww.mall.promotion.enums.GroupTradeType;
import com.ww.mall.promotion.mq.GroupMqConstant;
import com.ww.mall.promotion.mq.GroupAfterSaleSuccessMessage;
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
import java.util.Collections;

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
    private GroupQueryService groupQueryService;

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
     * 当内部状态变更消息发送失败且本地投影兜底也失败时，应登记投影补偿任务。
     */
    @Test
    void shouldSubmitProjectionCompensationTaskWhenFallbackProjectionAlsoFails() {
        doThrow(new RuntimeException("mq down")).when(rabbitMqPublisher).sendMsg(
                eq(GroupMqConstant.GROUP_EXCHANGE),
                eq(GroupMqConstant.GROUP_STATE_CHANGED_KEY),
                any()
        );
        doThrow(new RuntimeException("mongo down")).when(groupStorageComponent).syncProjection("group-1");

        ReflectionTestUtils.invokeMethod(groupCommandService, "afterStateChanged", "group-1", 1L);

        verify(groupStorageComponent).submitCompensationTask(
                eq(GroupCompensationTaskType.PROJECTION_SYNC),
                eq("group-1"),
                any(java.util.Date.class),
                anyString()
        );
    }

    /**
     * 当支付成功后被拼团业务规则拒绝时，应发送退款补偿消息而不是继续抛错重试。
     */
    @Test
    void shouldRequestRefundWhenPaidJoinRejectedByBusinessRule() {
        GroupOrderPaidMessage message = buildJoinPaidMessage();
        GroupActivity activity = buildJoinableActivity();
        activity.setEnabled(GroupEnabledStatus.DISABLED.isEnabled());

        when(groupStorageComponent.requireGroupSnapshot("group-1"))
                .thenReturn(buildSnapshotWithSpu("group-1", "activity-1", 2001L));
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
     * 创建拼团时，应根据命中的 SKU 规则把对应 SPU ID 透传给存储层。
     */
    @Test
    void shouldPassMatchedSpuIdToStorageWhenCreatingGroup() {
        GroupActivity activity = buildJoinableActivity();
        GroupCommandResult result = new GroupCommandResult();
        result.setSuccess(true);
        result.setGroupId("group-1");
        when(groupActivityCache.get("activity-1")).thenReturn(activity);
        when(groupStorageComponent.createGroup(anyString(), eq(activity),
                any(com.ww.mall.promotion.service.group.command.CreateGroupCommand.class), anyLong(), eq(2002L)))
                .thenReturn(result);
        when(groupQueryService.getGroupDetail("group-1")).thenReturn(new GroupInstanceVO());

        com.ww.mall.promotion.service.group.command.CreateGroupCommand command =
                new com.ww.mall.promotion.service.group.command.CreateGroupCommand();
        command.setGroupId("group-1");
        command.setActivityId("activity-1");
        command.setUserId(1L);
        command.setOrderId("order-1");
        command.setSkuId(2001L);
        command.setPayAmount(new BigDecimal("99.00"));

        groupCommandService.createGroup(command);

        verify(groupStorageComponent).createGroup(eq("group-1"), eq(activity), eq(command), anyLong(), eq(2002L));
    }

    /**
     * 开团支付成功时，应直接使用上游透传的 groupId 创建拼团。
     */
    @Test
    void shouldUseUpstreamGroupIdWhenHandlingStartPaidMessage() {
        GroupOrderPaidMessage message = buildStartPaidMessage();
        GroupActivity activity = buildJoinableActivity();
        GroupCommandResult result = new GroupCommandResult();
        result.setSuccess(true);
        result.setGroupId("group-start-1");

        when(groupActivityCache.get("activity-1")).thenReturn(activity);
        when(groupStorageComponent.createGroup(eq("group-start-1"), eq(activity),
                any(com.ww.mall.promotion.service.group.command.CreateGroupCommand.class), anyLong(), eq(2002L)))
                .thenReturn(result);
        when(groupQueryService.getGroupDetail("group-start-1")).thenReturn(new GroupInstanceVO());

        assertDoesNotThrow(() -> groupCommandService.handleOrderPaid(message));

        verify(groupStorageComponent).createGroup(eq("group-start-1"), eq(activity),
                any(com.ww.mall.promotion.service.group.command.CreateGroupCommand.class), anyLong(), eq(2002L));
    }

    /**
     * 支付成功消息缺少 groupId 时，应直接判定为非法消息。
     */
    @Test
    void shouldRejectPaidMessageWhenGroupIdMissing() {
        GroupOrderPaidMessage message = buildStartPaidMessage();
        message.setGroupId(null);

        assertThrows(ApiException.class, () -> groupCommandService.handleOrderPaid(message));

        verify(groupStorageComponent, never()).createGroup(
                anyString(),
                any(GroupActivity.class),
                any(com.ww.mall.promotion.service.group.command.CreateGroupCommand.class),
                anyLong(),
                anyLong()
        );
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

        when(groupStorageComponent.requireGroupSnapshot("group-1"))
                .thenReturn(buildSnapshotWithSpu("group-1", "activity-1", 2001L));
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
     * 当参团 SKU 命中的是活动下其他 SPU 时，应拒绝跨 SPU 混团。
     */
    @Test
    void shouldRejectJoinWhenSkuMatchesDifferentSpuWithinSameActivity() {
        GroupActivity activity = buildJoinableActivity();
        when(groupStorageComponent.requireGroupSnapshot("group-1"))
                .thenReturn(buildSnapshotWithSpu("group-1", "activity-1", 2001L));
        when(groupActivityCache.get("activity-1")).thenReturn(activity);

        com.ww.mall.promotion.service.group.command.JoinGroupCommand command =
                new com.ww.mall.promotion.service.group.command.JoinGroupCommand();
        command.setGroupId("group-1");
        command.setUserId(1L);
        command.setOrderId("order-join-1");
        command.setSkuId(2001L);
        command.setPayAmount(new BigDecimal("109.00"));

        ApiException exception = assertThrows(ApiException.class, () -> groupCommandService.joinGroup(command));

        assertEquals(com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_SPU_NOT_MATCH.getCode(),
                exception.getCode());
        verify(groupStorageComponent, never())
                .joinGroup(any(GroupActivity.class), any(com.ww.mall.promotion.service.group.command.JoinGroupCommand.class), anyLong());
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
     * 团已成功时，售后成功消息应直接跳过，不再进入拼团售后链路。
     */
    @Test
    void shouldSkipPromotionAfterSaleWhenGroupAlreadySuccess() {
        GroupAfterSaleSuccessMessage message = new GroupAfterSaleSuccessMessage();
        message.setGroupId("group-1");
        message.setOrderId("order-1");
        message.setUserId(1L);
        when(groupStorageComponent.loadGroupSnapshot("group-1")).thenReturn(buildSuccessSnapshot());

        assertDoesNotThrow(() -> groupCommandService.handleAfterSaleSuccess(message));

        verify(groupStorageComponent, never()).afterSaleSuccess(anyString(), anyString(), anyString(), anyLong(), anyString(), anyString());
        verify(rabbitMqPublisher, never()).sendMsg(
                eq(GroupMqConstant.GROUP_EXCHANGE),
                eq(GroupMqConstant.GROUP_REFUND_REQUEST_KEY),
                any()
        );
    }

    /**
     * 售后成功消息缺少 groupId 时，不再按订单反查拼团，应直接拒绝处理。
     */
    @Test
    void shouldRejectAfterSaleMessageWhenGroupIdMissing() {
        GroupAfterSaleSuccessMessage message = new GroupAfterSaleSuccessMessage();
        message.setOrderId("order-1");

        assertThrows(ApiException.class, () -> groupCommandService.handleAfterSaleSuccess(message));
    }

    /**
     * 当售后处理遇到并发状态变化导致 Lua 返回 no-op 时，不应继续发送状态变更消息。
     */
    @Test
    void shouldSkipStateChangedWhenAfterSaleBecomesNonOpenDuringLuaExecution() {
        GroupAfterSaleSuccessMessage message = new GroupAfterSaleSuccessMessage();
        message.setGroupId("group-1");
        message.setOrderId("order-1");
        message.setUserId(1L);
        when(groupStorageComponent.loadGroupSnapshot("group-1")).thenReturn(buildOpenSnapshot());
        when(groupStorageComponent.afterSaleSuccess(eq("group-1"), nullable(String.class), eq("order-1"),
                anyLong(), anyString(), nullable(String.class)))
                .thenReturn(3);

        assertDoesNotThrow(() -> groupCommandService.handleAfterSaleSuccess(message));

        verify(rabbitMqPublisher, never()).sendMsg(
                eq(GroupMqConstant.GROUP_EXCHANGE),
                eq(GroupMqConstant.GROUP_STATE_CHANGED_KEY),
                any()
        );
        verify(rabbitMqPublisher, never()).sendMsg(
                eq(GroupMqConstant.GROUP_EXCHANGE),
                eq(GroupMqConstant.GROUP_REFUND_REQUEST_KEY),
                any()
        );
    }

    /**
     * 当失败拼团仍存在待退款成员时，应支持人工重发退款补偿。
     */
    @Test
    void shouldTriggerPendingRefundCompensationManually() {
        when(groupStorageComponent.requireGroupSnapshot("group-1")).thenReturn(buildFailedSnapshot());

        int publishedCount = groupCommandService.triggerPendingRefundCompensation("group-1", "人工补偿");

        assertEquals(1, publishedCount);
        verify(rabbitMqPublisher).sendMsg(
                eq(GroupMqConstant.GROUP_EXCHANGE),
                eq(GroupMqConstant.GROUP_REFUND_REQUEST_KEY),
                any()
        );
    }

    /**
     * 当待退款补偿消息发送失败时，应登记退款补偿任务等待定时任务重试。
     */
    @Test
    void shouldSubmitRefundRetryTaskWhenPendingRefundMessageSendFails() {
        when(groupStorageComponent.requireGroupSnapshot("group-1")).thenReturn(buildFailedSnapshot());
        doThrow(new RuntimeException("mq down")).when(rabbitMqPublisher).sendMsg(
                eq(GroupMqConstant.GROUP_EXCHANGE),
                eq(GroupMqConstant.GROUP_REFUND_REQUEST_KEY),
                any()
        );

        int publishedCount = groupCommandService.triggerPendingRefundCompensation("group-1", "人工补偿");

        assertEquals(0, publishedCount);
        verify(groupStorageComponent).submitCompensationTask(
                eq(GroupCompensationTaskType.REFUND_RETRY),
                eq("group-1"),
                any(java.util.Date.class),
                anyString()
        );
    }

    /**
     * 当失败拼团不存在待退款成员时，待退款检查应返回 false。
     */
    @Test
    void shouldReturnFalseWhenNoPendingRefundMembers() {
        GroupCacheSnapshot snapshot = buildFailedSnapshot();
        snapshot.getMembers().forEach(member -> member.setMemberStatus("SUCCESS"));
        when(groupStorageComponent.requireGroupSnapshot("group-1")).thenReturn(snapshot);

        assertFalse(groupCommandService.hasPendingRefund("group-1"));
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
     * 构造开团支付成功消息。
     *
     * @return 支付消息
     */
    private GroupOrderPaidMessage buildStartPaidMessage() {
        GroupOrderPaidMessage message = new GroupOrderPaidMessage();
        message.setTradeType(GroupTradeType.START);
        message.setActivityId("activity-1");
        message.setGroupId("group-start-1");
        message.setOrderId("order-start-1");
        message.setUserId(1L);
        message.setSkuId(2001L);
        message.setPayAmount(new BigDecimal("109.00"));
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
        activity.setEnabled(GroupEnabledStatus.ENABLED.isEnabled());
        activity.setRequiredSize(2);
        activity.setExpireHours(24);
        GroupActivity.GroupSpuConfig firstSpuConfig = new GroupActivity.GroupSpuConfig();
        firstSpuConfig.setSpuId(2001L);
        GroupActivity.GroupSkuRule firstSkuRule = new GroupActivity.GroupSkuRule();
        firstSkuRule.setSkuId(1001L);
        firstSkuRule.setGroupPrice(new BigDecimal("99.00"));
        firstSkuRule.setEnabled(true);
        firstSpuConfig.setSkuRules(Collections.singletonList(firstSkuRule));

        GroupActivity.GroupSpuConfig secondSpuConfig = new GroupActivity.GroupSpuConfig();
        secondSpuConfig.setSpuId(2002L);
        GroupActivity.GroupSkuRule secondSkuRule = new GroupActivity.GroupSkuRule();
        secondSkuRule.setSkuId(2001L);
        secondSkuRule.setGroupPrice(new BigDecimal("109.00"));
        secondSkuRule.setEnabled(true);
        secondSpuConfig.setSkuRules(Collections.singletonList(secondSkuRule));

        activity.setSpuConfigs(Arrays.asList(firstSpuConfig, secondSpuConfig));
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
     * 构造带有团内 SPU 绑定关系的参团前快照。
     *
     * @param groupId 团ID
     * @param activityId 活动ID
     * @param spuId 团已绑定的 SPU ID
     * @return 团快照
     */
    private GroupCacheSnapshot buildSnapshotWithSpu(String groupId, String activityId, Long spuId) {
        GroupCacheSnapshot snapshot = new GroupCacheSnapshot();
        GroupInstance instance = new GroupInstance();
        instance.setId(groupId);
        instance.setActivityId(activityId);
        instance.setSpuId(spuId);
        snapshot.setInstance(instance);
        return snapshot;
    }

    /**
     * 构造进行中的团快照。
     *
     * @return 团快照
     */
    private GroupCacheSnapshot buildOpenSnapshot() {
        GroupCacheSnapshot snapshot = new GroupCacheSnapshot();
        GroupInstance instance = new GroupInstance();
        instance.setId("group-1");
        instance.setActivityId("activity-1");
        instance.setLeaderUserId(1L);
        instance.setStatus(GroupStatus.OPEN.getCode());
        snapshot.setInstance(instance);

        GroupMember leader = new GroupMember();
        leader.setUserId(1L);
        leader.setOrderId("order-1");
        leader.setMemberStatus("JOINED");
        snapshot.setMembers(Collections.singletonList(leader));
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
        instance.setStatus(GroupStatus.FAILED.getCode());
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

    /**
     * 构造拼团成功快照。
     *
     * @return 成功快照
     */
    private GroupCacheSnapshot buildSuccessSnapshot() {
        GroupCacheSnapshot snapshot = new GroupCacheSnapshot();
        GroupInstance instance = new GroupInstance();
        instance.setId("group-1");
        instance.setActivityId("activity-1");
        instance.setStatus(GroupStatus.SUCCESS.getCode());
        snapshot.setInstance(instance);
        return snapshot;
    }
}
