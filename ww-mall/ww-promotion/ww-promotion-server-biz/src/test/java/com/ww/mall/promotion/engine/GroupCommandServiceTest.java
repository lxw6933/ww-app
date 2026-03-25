package com.ww.mall.promotion.engine;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.app.rabbitmq.RabbitMqPublisher;
import com.ww.mall.promotion.component.GroupStorageComponent;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.mq.GroupMqConstant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
}
