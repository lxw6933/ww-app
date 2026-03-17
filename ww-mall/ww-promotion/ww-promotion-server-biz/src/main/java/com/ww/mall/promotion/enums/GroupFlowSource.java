package com.ww.mall.promotion.enums;

import lombok.Getter;

/**
 * 拼团链路记录来源。
 *
 * @author ww
 * @create 2026-03-17
 * @description: 统一 group_flow_log 的来源枚举
 */
@Getter
public enum GroupFlowSource {

    /**
     * 拼团服务入口。
     */
    GROUP_SERVICE,

    /**
     * 拼团支付回调编排层。
     */
    GROUP_PAYMENT_CALLBACK,

    /**
     * 拼团异步处理器。
     */
    GROUP_PROCESSOR,

    /**
     * 拼团 MQ 消费者。
     */
    GROUP_MQ_CONSUMER,

    /**
     * 拼团定时任务。
     */
    GROUP_JOB
}
