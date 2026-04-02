package com.ww.mall.promotion.component;

import com.ww.mall.promotion.engine.model.GroupCommandResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 拼团存储组件测试。
 * <p>
 * 该测试聚焦 Redis Lua 返回值到 Java 结果对象的映射语义，
 * 避免把“事件类型”误写入“团状态”字段，导致后续调用方误判。
 *
 * @author ww
 * @create 2026-04-02
 * @description: 校验参团 Lua 返回结果解析逻辑
 */
class GroupStorageComponentTest {

    /**
     * 当参团脚本返回成功事件时，应把第三个返回值解析为事件类型，
     * 而不应错误写入真实团状态字段。
     */
    @Test
    void shouldParseJoinSuccessEventTypeSeparatelyFromGroupStatus() {
        GroupStorageComponent storageComponent = new GroupStorageComponent();

        GroupCommandResult result = ReflectionTestUtils.invokeMethod(
                storageComponent,
                "parseJoinResult",
                Arrays.asList(1, "group-1", "GROUP_COMPLETED", "2", "0")
        );

        assertTrue(result.isSuccess());
        assertEquals("group-1", result.getGroupId());
        assertEquals("GROUP_COMPLETED", result.getEventType());
        assertNull(result.getGroupStatus());
    }
}
