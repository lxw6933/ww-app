package com.ww.mall.promotion.key;

import com.ww.app.redis.key.RedisKeyBuilder;
import org.springframework.stereotype.Component;

/**
 * 拼团 Redis Key 构建器。
 * <p>
 * 新版拼团重构后，Redis 为主状态存储，所有命令链路都围绕统一 Key 规范运转。
 * 这里集中维护各类状态、索引和计数器 Key，避免命令脚本与 Java 代码各自拼串。
 * <p>
 * 当前拼团主链路固定使用 6 个核心 Key：
 * 1. 团主状态：{@code group:instance:meta:{groupId}}
 * 2. 团成员仓库：{@code group:instance:member-store:{groupId}}
 * 3. 团内活跃用户索引：{@code group:instance:user-index:{groupId}}
 * 4. 全局订单幂等索引：{@code group:order:index}
 * 5. 活动用户占位计数：{@code group:activity:active:count}
 * 6. 过期调度索引：{@code group:expiry}
 * <p>
 * 示例：
 * 团状态 Key:
 * {@code promotion:group:instance:meta:67dd3ac8f5a6f80001a10001}
 * <p>
 * 团状态 Value:
 * {@code {status=OPEN,currentSize=1,remainingSlots=1,expireTime=1770000000000}}
 *
 * @author ww
 * @create 2026-03-19
 * @description: 拼团 Redis Key 构建器
 */
@Component
public class GroupRedisKeyBuilder extends RedisKeyBuilder {

    private static final String GROUP = "group";
    private static final String INSTANCE = "instance";
    private static final String META = "meta";
    private static final String ORDER = "order";
    private static final String INDEX = "index";
    private static final String ACTIVITY = "activity";
    private static final String ACTIVE = "active";
    private static final String COUNT = "count";
    private static final String STATS = "stats";
    private static final String EXPIRY = "expiry";
    private static final String MEMBER_STORE = "member-store";
    private static final String USER_INDEX = "user-index";

    /**
     * 团主状态 Hash Key。
     * <p>
     * 功能：
     * 保存拼团聚合根的主状态，所有参团、售后、过期、成团判断都先读取该 Key。
     * <p>
     * Key 示例：
     * {@code promotion:group:instance:meta:67dd3ac8f5a6f80001a10001}
     * <p>
     * Value 示例：
     * {@code {activityId=ACT_1001,status=OPEN,currentSize=1,remainingSlots=1,leaderUserId=20001}}
     *
     * @param groupId 团ID
     * @return Redis Key
     */
    public String buildGroupMetaKey(String groupId) {
        return join(GROUP, INSTANCE, META, groupId);
    }

    /**
     * 团成员明细存储 Hash Key。
     * <p>
     * 功能：
     * 以 {@code orderId} 作为 field，保存成员完整 JSON 快照。
     * 同一个团的所有成员都收敛在一个 Hash 内，避免“一成员一 Key”导致 Key 爆炸。
     * <p>
     * Key 示例：
     * {@code promotion:group:instance:member-store:67dd3ac8f5a6f80001a10001}
     * <p>
     * Field 示例：
     * {@code ORDER_10001}
     * <p>
     * Value 示例：
     * {@code {"userId":20001,"orderId":"ORDER_10001","skuId":30001,"memberStatus":"JOINED","payAmount":99.00}}
     *
     * @param groupId 团ID
     * @return Redis Key
     */
    public String buildGroupMemberStoreKey(String groupId) {
        return join(GROUP, INSTANCE, MEMBER_STORE, groupId);
    }

    /**
     * 团内活跃用户索引 Hash Key。
     * <p>
     * field 为 userId，value 为 orderId，仅保存当前仍占用拼团资格的成员。
     * <p>
     * 功能：
     * 在参团阶段做 O(1) 的“团内是否已参团”判断。
     * 售后释放名额、过期关团、团长售后关团时，会同步删除对应 field。
     * <p>
     * Key 示例：
     * {@code promotion:group:instance:user-index:67dd3ac8f5a6f80001a10001}
     * <p>
     * Field/Value 示例：
     * {@code 20001 -> ORDER_10001}
     *
     * @param groupId 团ID
     * @return Redis Key
     */
    public String buildGroupUserIndexKey(String groupId) {
        return join(GROUP, INSTANCE, USER_INDEX, groupId);
    }

    /**
     * 全局订单索引 Hash Key。
     * <p>
     * field 为 orderId，value 为 groupId，用于支付消息幂等回放与售后反查。
     * <p>
     * 功能：
     * 同一个订单无论支付成功消息被投递多少次，都先命中该索引完成幂等回放。
     * 同时售后消息如果只带 {@code orderId} 不带 {@code groupId}，也依赖该索引反查所属拼团。
     * <p>
     * Key 示例：
     * {@code promotion:group:order:index}
     * <p>
     * Field/Value 示例：
     * {@code ORDER_10001 -> 67dd3ac8f5a6f80001a10001}
     *
     * @return Redis Key
     */
    public String buildOrderIndexKey() {
        return join(GROUP, ORDER, INDEX);
    }

    /**
     * 全局活动用户有效名额计数 Hash Key。
     * <p>
     * 功能：
     * 控制“同一活动下一个用户当前最多占用多少个有效拼团名额”。
     * 开团/参团时加一，售后释放或拼团失败时减一。
     * <p>
     * Key 示例：
     * {@code promotion:group:activity:active:count}
     * <p>
     * Field/Value 示例：
     * {@code ACT_1001:20001 -> 2}
     *
     * @return Redis Key
     */
    public String buildActivityUserCountKey() {
        return join(GROUP, ACTIVITY, ACTIVE, COUNT);
    }

    /**
     * 构建活动用户计数字段。
     * <p>
     * 示例：
     * {@code ACT_1001:20001}
     *
     * @param activityId 活动ID
     * @param userId 用户ID
     * @return Hash field
     */
    public String buildActivityUserCountField(String activityId, Long userId) {
        return activityId + SPLIT_ITEM + userId;
    }

    /**
     * 构建活动用户计数字段前缀。
     * <p>
     * 主要用于 Lua 批量释放名额时拼接用户ID。
     * <p>
     * 示例：
     * {@code ACT_1001:}
     *
     * @param activityId 活动ID
     * @return Hash field 前缀
     */
    public String buildActivityUserCountFieldPrefix(String activityId) {
        return activityId + SPLIT_ITEM;
    }

    /**
     * 活动统计 Hash Key。
     * <p>
     * 功能：
     * 记录活动维度的累计开团数与累计参团人数。
     * 当前仅维护两个累计指标：
     * 1. {@code openGroupCount}：首次开团成功次数
     * 2. {@code joinMemberCount}：累计参团人数，团长开团也计入一次
     * <p>
     * Key 示例：
     * {@code promotion:group:activity:stats:ACT_1001}
     *
     * @param activityId 活动ID
     * @return Redis Key
     */
    public String buildActivityStatsKey(String activityId) {
        return join(GROUP, ACTIVITY, STATS, activityId);
    }

    /**
     * 过期索引 Key。
     * <p>
     * 功能：
     * 使用 ZSet 的 score 存团过期毫秒值，定时任务只需要按时间窗口扫描即可。
     * <p>
     * Key 示例：
     * {@code promotion:group:expiry}
     * <p>
     * Member/Score 示例：
     * {@code 67dd3ac8f5a6f80001a10001 -> 1770000000000}
     *
     * @return Redis Key
     */
    public String buildExpiryIndexKey() {
        return join(GROUP, EXPIRY);
    }

    /**
     * 使用统一前缀与分隔符拼接 Key。
     *
     * @param parts 片段
     * @return Redis Key
     */
    private String join(String... parts) {
        return getPrefix() + String.join(SPLIT_ITEM, parts);
    }
}
