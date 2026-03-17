package com.ww.mall.promotion.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 拼团成员业务状态。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 细化成员在拼团域内的生命周期，便于客服回放与售后处理
 */
@Getter
public enum GroupMemberBizStatus {

    /**
     * 已支付并占位，等待成团。
     */
    JOINED,

    /**
     * 已成团。
     */
    SUCCESS,

    /**
     * 售后成功，名额已归还。
     */
    AFTER_SALE_RELEASED,

    /**
     * 团失败后待退款。
     */
    FAILED_REFUND_PENDING,

    /**
     * 团长售后导致团关闭。
     */
    LEADER_AFTER_SALE_CLOSED;

    /**
     * 返回当前仍占用业务资格的状态列表。
     *
     * @return 状态编码列表
     */
    public static List<String> activeStatuses() {
        return Arrays.stream(values())
                .filter(item -> item == JOINED || item == SUCCESS)
                .map(Enum::name)
                .collect(Collectors.toList());
    }
}
