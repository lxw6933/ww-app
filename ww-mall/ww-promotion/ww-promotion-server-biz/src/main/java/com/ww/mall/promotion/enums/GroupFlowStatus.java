package com.ww.mall.promotion.enums;

import lombok.Getter;

/**
 * 拼团链路记录状态。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 统一 group_flow_log 的状态枚举
 */
@Getter
public enum GroupFlowStatus {

    /**
     * 处理中。
     */
    PROCESSING,

    /**
     * 成功。
     */
    SUCCESS,

    /**
     * 失败。
     */
    FAILED,

    /**
     * 跳过。
     */
    SKIPPED
}
