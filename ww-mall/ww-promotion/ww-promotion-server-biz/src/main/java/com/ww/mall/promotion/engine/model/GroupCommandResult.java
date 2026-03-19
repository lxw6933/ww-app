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
     * 失败原因。
     */
    private String failReason;
}
