package com.ww.mall.promotion.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ww.app.common.exception.ApiException;
import com.ww.mall.promotion.engine.model.GroupCacheSnapshot;
import com.ww.mall.promotion.engine.model.GroupMemberCacheSnapshot;
import com.ww.mall.promotion.entity.group.GroupInstance;
import com.ww.mall.promotion.entity.group.GroupMember;
import com.ww.mall.promotion.key.GroupRedisKeyBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ww.mall.promotion.constants.ErrorCodeConstants.GROUP_RECORD_NOT_EXISTS;

/**
 * 拼团 Redis 状态读取器。
 *
 * @author ww
 * @create 2026-03-19
 * @description: 从 Redis 主状态恢复拼团完整快照
 */
@Component
public class GroupRedisStateReader {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private GroupRedisKeyBuilder groupRedisKeyBuilder;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 读取拼团快照。
     *
     * @param groupId 团ID
     * @return 快照，不存在时返回null
     */
    public GroupCacheSnapshot loadGroupSnapshot(String groupId) {
        if (!hasText(groupId)) {
            return null;
        }
        Map<Object, Object> metaMap = stringRedisTemplate.opsForHash().entries(groupRedisKeyBuilder.buildGroupMetaKey(groupId));
        if (metaMap == null || metaMap.isEmpty()) {
            return null;
        }
        GroupCacheSnapshot snapshot = new GroupCacheSnapshot();
        List<GroupMember> members = loadMembers(groupId);
        GroupInstance instance = buildInstance(groupId, metaMap, members);
        snapshot.setInstance(instance);
        snapshot.setMembers(members);
        return snapshot;
    }

    /**
     * 必须存在的拼团快照。
     *
     * @param groupId 团ID
     * @return 拼团快照
     */
    public GroupCacheSnapshot requireGroupSnapshot(String groupId) {
        GroupCacheSnapshot snapshot = loadGroupSnapshot(groupId);
        if (snapshot == null || snapshot.getInstance() == null) {
            throw new ApiException(GROUP_RECORD_NOT_EXISTS);
        }
        return snapshot;
    }

    /**
     * 读取成员快照。
     *
     * @param groupId 团ID
     * @return 成员列表
     */
    private List<GroupMember> loadMembers(String groupId) {
        List<Object> memberJsonList = stringRedisTemplate.opsForHash()
                .values(groupRedisKeyBuilder.buildGroupMemberStoreKey(groupId));
        if (memberJsonList == null || memberJsonList.isEmpty()) {
            return new ArrayList<>();
        }
        List<GroupMember> members = new ArrayList<>();
        for (Object memberJson : memberJsonList) {
            if (memberJson == null) {
                continue;
            }
            members.add(buildMember(String.valueOf(memberJson)));
        }
        members.sort((left, right) -> compareDate(left.getJoinTime(), right.getJoinTime()));
        return members;
    }

    /**
     * 构建团实例。
     *
     * @param groupId 团ID
     * @param metaMap 主状态Hash
     * @param members 成员列表
     * @return 团实例
     */
    private GroupInstance buildInstance(String groupId, Map<Object, Object> metaMap, List<GroupMember> members) {
        GroupInstance instance = new GroupInstance();
        instance.setId(groupId);
        instance.setActivityId(getString(metaMap, "activityId"));
        instance.setLeaderUserId(getLong(metaMap, "leaderUserId"));
        instance.setStatus(getString(metaMap, "status"));
        instance.setRequiredSize(getInteger(metaMap, "requiredSize"));
        instance.setCurrentSize(getInteger(metaMap, "currentSize"));
        instance.setRemainingSlots(getInteger(metaMap, "remainingSlots"));
        instance.setExpireTime(getDate(metaMap, "expireTime"));
        instance.setCompleteTime(getDate(metaMap, "completeTime"));
        instance.setFailedTime(getDate(metaMap, "failedTime"));
        instance.setSpuId(getLong(metaMap, "spuId"));
        instance.setFailReason(getString(metaMap, "failReason"));
        instance.setCreateTime(getDate(metaMap, "createTime"));
        instance.setUpdateTime(getDate(metaMap, "updateTime"));
        instance.setSkuIds(members.stream()
                .map(GroupMember::getSkuId)
                .filter(item -> item != null)
                .distinct()
                .collect(Collectors.toList()));
        instance.setMembers(members.stream()
                .map(this::buildMemberSummary)
                .collect(Collectors.toList()));
        return instance;
    }

    /**
     * 构建成员对象。
     *
     * @param memberJson 成员JSON
     * @return 成员对象
     */
    private GroupMember buildMember(String memberJson) {
        try {
            GroupMemberCacheSnapshot cacheSnapshot = objectMapper.readValue(memberJson, GroupMemberCacheSnapshot.class);
            GroupMember member = new GroupMember();
            member.setGroupInstanceId(cacheSnapshot.getGroupInstanceId());
            member.setUserId(cacheSnapshot.getUserId());
            member.setOrderId(cacheSnapshot.getOrderId());
            member.setIsLeader(cacheSnapshot.getIsLeader());
            member.setJoinTime(toDate(cacheSnapshot.getJoinTime()));
            member.setPayAmount(cacheSnapshot.getPayAmount());
            member.setSkuId(cacheSnapshot.getSkuId());
            member.setMemberStatus(cacheSnapshot.getMemberStatus());
            member.setAfterSaleId(cacheSnapshot.getAfterSaleId());
            member.setLatestTrajectory(cacheSnapshot.getLatestTrajectory());
            member.setLatestTrajectoryTime(toDate(cacheSnapshot.getLatestTrajectoryTime()));
            member.setCreateTime(member.getJoinTime());
            member.setUpdateTime(member.getLatestTrajectoryTime() != null ? member.getLatestTrajectoryTime() : member.getJoinTime());
            return member;
        } catch (Exception e) {
            throw new IllegalStateException("拼团成员缓存反序列化失败", e);
        }
    }

    /**
     * 构建成员摘要。
     *
     * @param member 成员
     * @return 摘要
     */
    private GroupInstance.GroupMemberInfo buildMemberSummary(GroupMember member) {
        GroupInstance.GroupMemberInfo memberInfo = new GroupInstance.GroupMemberInfo();
        memberInfo.setUserId(member.getUserId());
        memberInfo.setOrderId(member.getOrderId());
        memberInfo.setSkuId(member.getSkuId());
        memberInfo.setJoinTime(member.getJoinTime());
        memberInfo.setIsLeader(member.getIsLeader() != null && member.getIsLeader() == 1);
        memberInfo.setMemberStatus(member.getMemberStatus());
        memberInfo.setLatestTrajectory(member.getLatestTrajectory());
        memberInfo.setLatestTrajectoryTime(member.getLatestTrajectoryTime());
        return memberInfo;
    }

    /**
     * 比较时间。
     *
     * @param left 左值
     * @param right 右值
     * @return 比较结果
     */
    private int compareDate(Date left, Date right) {
        Date leftValue = left != null ? left : new Date(0L);
        Date rightValue = right != null ? right : new Date(0L);
        return leftValue.compareTo(rightValue);
    }

    /**
     * 毫秒值转日期。
     *
     * @param millis 毫秒值
     * @return 日期
     */
    private Date toDate(Long millis) {
        return millis == null || millis <= 0 ? null : new Date(millis);
    }

    /**
     * 读取字符串。
     *
     * @param map 数据Map
     * @param key 字段名
     * @return 字符串值
     */
    private String getString(Map<Object, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 读取长整型。
     *
     * @param map 数据Map
     * @param key 字段名
     * @return 长整型值
     */
    private Long getLong(Map<Object, Object> map, String key) {
        String value = getString(map, key);
        return hasText(value) ? Long.parseLong(value) : null;
    }

    /**
     * 读取整型。
     *
     * @param map 数据Map
     * @param key 字段名
     * @return 整型值
     */
    private Integer getInteger(Map<Object, Object> map, String key) {
        String value = getString(map, key);
        return hasText(value) ? Integer.parseInt(value) : null;
    }

    /**
     * 读取日期。
     *
     * @param map 数据Map
     * @param key 字段名
     * @return 日期
     */
    private Date getDate(Map<Object, Object> map, String key) {
        Long millis = getLong(map, key);
        return millis == null || millis <= 0 ? null : new Date(millis);
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
