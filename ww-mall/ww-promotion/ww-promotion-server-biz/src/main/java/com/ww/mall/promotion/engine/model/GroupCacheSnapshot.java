package com.ww.mall.promotion.engine.model;

import com.ww.mall.promotion.entity.group.GroupInstance;
import com.ww.mall.promotion.entity.group.GroupMember;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 拼团 Redis 快照。
 *
 * @author ww
 * @create 2026-03-19
 * @description: 从 Redis 主状态恢复出的完整拼团快照
 */
@Data
public class GroupCacheSnapshot {

    /**
     * 团主文档快照。
     */
    private GroupInstance instance;

    /**
     * 成员快照列表。
     */
    private List<GroupMember> members = new ArrayList<>();
}
