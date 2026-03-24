package com.ww.mall.promotion.engine.projection;

import com.ww.mall.promotion.engine.GroupRedisStateReader;
import com.ww.mall.promotion.engine.model.GroupCacheSnapshot;
import com.ww.mall.promotion.entity.group.GroupInstance;
import com.ww.mall.promotion.entity.group.GroupMember;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 拼团 Mongo 投影持久化服务。
 * <p>
 * Mongo 查询模型由命令链路在 Redis Lua 成功后直接同步最新快照，
 * 不再依赖额外的异步投影组件。
 *
 * @author ww
 * @create 2026-03-24
 * @description: 负责把 Redis 快照同步为 Mongo 查询模型
 */
@Service
public class GroupProjectionPersistenceService {

    @Resource
    private GroupRedisStateReader groupRedisStateReader;

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 根据团ID从 Redis 读取最新快照并同步到 Mongo。
     *
     * @param groupId 团ID
     * @return 同步后的快照
     */
    public GroupCacheSnapshot syncSnapshot(String groupId) {
        GroupCacheSnapshot snapshot = groupRedisStateReader.requireGroupSnapshot(groupId);
        syncSnapshot(snapshot);
        return snapshot;
    }

    /**
     * 直接同步指定快照到 Mongo。
     *
     * @param snapshot 拼团快照
     */
    public void syncSnapshot(GroupCacheSnapshot snapshot) {
        if (snapshot == null || snapshot.getInstance() == null) {
            return;
        }
        mongoTemplate.save(snapshot.getInstance());
        upsertMembers(snapshot.getMembers());
    }

    /**
     * 从 Mongo 查询模型回读完整快照。
     *
     * @param groupId 团ID
     * @return Mongo 快照，不存在时返回 {@code null}
     */
    public GroupCacheSnapshot loadSnapshotFromMongo(String groupId) {
        GroupInstance instance = mongoTemplate.findOne(GroupInstance.buildIdQuery(groupId), GroupInstance.class);
        if (instance == null) {
            return null;
        }
        List<GroupMember> members = mongoTemplate.find(GroupMember.buildGroupInstanceIdQuery(groupId), GroupMember.class);
        GroupCacheSnapshot snapshot = new GroupCacheSnapshot();
        snapshot.setInstance(instance);
        snapshot.setMembers(members);
        return snapshot;
    }

    /**
     * 按订单号增量 upsert 成员轨迹。
     *
     * @param members 成员快照
     */
    private void upsertMembers(List<GroupMember> members) {
        if (members == null || members.isEmpty()) {
            return;
        }
        BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, GroupMember.class);
        Date now = new Date();
        int upsertCount = 0;
        for (GroupMember member : members) {
            if (member == null || !hasText(member.getOrderId())) {
                continue;
            }
            bulkOperations.upsert(
                    GroupMember.buildOrderIdQuery(member.getOrderId()),
                    GroupMember.buildOrderIdUpsert(member, now)
            );
            upsertCount++;
        }
        if (upsertCount > 0) {
            bulkOperations.execute();
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
