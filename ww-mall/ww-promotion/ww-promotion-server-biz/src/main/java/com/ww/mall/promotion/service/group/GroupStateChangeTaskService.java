package com.ww.mall.promotion.service.group;

import com.ww.mall.promotion.mq.GroupStateChangedMessage;

/**
 * 拼团状态变更任务服务。
 *
 * @author ww
 * @create 2026-03-25
 * @description: 负责处理拼团状态变更内部消息
 */
public interface GroupStateChangeTaskService {

    /**
     * 处理拼团状态变更内部消息。
     *
     * @param message 状态变更内部消息
     */
    void handleStateChanged(GroupStateChangedMessage message);
}
