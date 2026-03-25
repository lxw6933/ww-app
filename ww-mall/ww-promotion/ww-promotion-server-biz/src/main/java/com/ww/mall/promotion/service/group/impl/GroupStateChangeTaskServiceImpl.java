package com.ww.mall.promotion.service.group.impl;

import com.ww.mall.promotion.engine.GroupStorageComponent;
import com.ww.mall.promotion.engine.model.GroupCacheSnapshot;
import com.ww.mall.promotion.mq.GroupStateChangedMessage;
import com.ww.mall.promotion.service.group.GroupStateChangeTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 拼团状态变更消息处理服务实现。
 * <p>
 * 主链路只负责投递一条内部状态变更 MQ，消费者收到消息后只负责把
 * Redis 快照同步落到 Mongo 查询模型，避免同步链路承担过多副作用。
 *
 * @author ww
 * @create 2026-03-25
 * @description: 拼团状态变更消息处理服务实现
 */
@Slf4j
@Service
public class GroupStateChangeTaskServiceImpl implements GroupStateChangeTaskService {

    @Resource
    private GroupStorageComponent groupStorageComponent;

    @Override
    public void handleStateChanged(GroupStateChangedMessage message) {
        if (message == null || !hasText(message.getGroupId())) {
            return;
        }
        try {
            GroupCacheSnapshot snapshot = groupStorageComponent.syncProjection(message.getGroupId());
            if (snapshot == null || snapshot.getInstance() == null) {
                log.warn("拼团状态变更内部消息未读取到有效快照: groupId={}",
                        message.getGroupId());
            }
        } catch (Exception e) {
            log.error("拼团状态变更内部消息处理失败: groupId={}",
                    message.getGroupId(), e);
        }
    }

    /**
     * 判断文本是否有值。
     *
     * @param value 文本
     * @return true-有值
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
