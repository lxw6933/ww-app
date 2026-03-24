package com.ww.mall.promotion.enums;

/**
 * 拼团领域事件类型。
 *
 * @author ww
 * @create 2026-03-19
 * @description: 拼团最终态对外通知事件类型枚举
 */
public enum GroupNotifyEventType {

    /**
     * 参团后达到门槛并成团。
     */
    GROUP_COMPLETED,

    /**
     * 拼团失败关闭。
     */
    GROUP_FAILED
}
