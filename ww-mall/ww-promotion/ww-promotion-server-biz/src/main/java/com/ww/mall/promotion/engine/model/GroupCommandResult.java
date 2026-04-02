package com.ww.mall.promotion.engine.model;

import lombok.Data;

/**
 * 拼团命令执行结果。
 *
 * @author ww
 * @create 2026-03-19
 * @description: Lua 命令执行后的统一返回对象
 */
@Data
public class GroupCommandResult {

    /**
     * 是否执行成功。
     */
    private boolean success;

    /**
     * 命中的拼团ID。
     */
    private String groupId;

    /**
     * 是否为幂等回放。
     */
    private boolean replayed;

    /**
     * 团当前状态。
     */
    private String groupStatus;

    /**
     * 本次命令触发的事件类型。
     * <p>
     * 例如参团脚本成功时返回的 {@code GROUP_JOINED}、{@code GROUP_COMPLETED}，
     * 该字段仅表达“本次状态迁移事件”，不等同于团聚合的真实状态。
     */
    private String eventType;

    /**
     * 失败原因。
     */
    private String failReason;
}
